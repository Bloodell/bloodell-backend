package br.edu.cesar.bloodell.telemetria.infraestrutura;

import br.edu.cesar.bloodell.config.FiltroDeCorrelacao;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.telemetria.aplicacao.GravadorDeLeituras;
import br.edu.cesar.bloodell.telemetria.infraestrutura.FilaDeIngestao.LeituraEmTransito;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ProcessadorDeTelemetria {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessadorDeTelemetria.class);
    private static final long ESPERA_NA_FILA_MS = 200;

    private final FilaDeIngestao fila;
    private final GravadorDeLeituras gravador;
    private final MetricasDeIngestao metricas;
    private final PropriedadesBloodell propriedades;

    private final AtomicBoolean ativo = new AtomicBoolean(false);
    private ExecutorService pool;

    public ProcessadorDeTelemetria(FilaDeIngestao fila,
                                   GravadorDeLeituras gravador,
                                   MetricasDeIngestao metricas,
                                   PropriedadesBloodell propriedades) {
        this.fila = fila;
        this.gravador = gravador;
        this.metricas = metricas;
        this.propriedades = propriedades;
    }

    @PostConstruct
    public void iniciar() {
        int quantidade = propriedades.getTelemetria().getQuantidadeWorkers();
        ativo.set(true);
        pool = Executors.newFixedThreadPool(quantidade, tarefa -> {
            Thread thread = new Thread(tarefa);
            thread.setName("telemetria-worker-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        });
        for (int indice = 0; indice < quantidade; indice++) {
            pool.submit(this::consumir);
        }
        LOG.info("Ingestao de telemetria iniciada com {} worker(s), fila de {} posicoes, lote de {}.",
                quantidade, fila.getCapacidade(), propriedades.getTelemetria().getTamanhoLote());
    }

    @PreDestroy
    public void parar() {
        ativo.set(false);
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                LOG.warn("Workers de telemetria nao encerraram no prazo; forcando parada.");
                pool.shutdownNow();
            }
        } catch (InterruptedException interrupcao) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
        LOG.info("Ingestao de telemetria encerrada. Persistidas: {} leitura(s).", metricas.getPersistidas());
    }

    private void consumir() {
        List<LeituraEmTransito> lote = new ArrayList<>();
        int tamanhoMaximo = propriedades.getTelemetria().getTamanhoLote();
        while (ativo.get() || fila.tamanhoAtual() > 0) {
            lote.clear();
            try {
                int quantidade = fila.drenarLote(lote, tamanhoMaximo, ESPERA_NA_FILA_MS);
                if (quantidade == 0) {
                    continue;
                }
                MDC.put(FiltroDeCorrelacao.CHAVE_MDC, lote.get(0).correlacao());
                long inicio = System.nanoTime();
                int gravadas = gravador.gravarLote(List.copyOf(lote));
                long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
                metricas.contarLote(gravadas, duracaoMs);
                LOG.debug("Lote de {} leitura(s) gravado em {} ms.", gravadas, duracaoMs);
            } catch (InterruptedException interrupcao) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException falha) {
                LOG.error("Falha ao gravar lote de telemetria; o lote foi descartado.", falha);
            } finally {
                MDC.remove(FiltroDeCorrelacao.CHAVE_MDC);
            }
        }
    }

    public boolean aguardarFilaVazia(long tempoMaximoMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + tempoMaximoMs;
        while (System.currentTimeMillis() < limite) {
            if (fila.tamanhoAtual() == 0) {
                Thread.sleep(300);
                return fila.tamanhoAtual() == 0;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
