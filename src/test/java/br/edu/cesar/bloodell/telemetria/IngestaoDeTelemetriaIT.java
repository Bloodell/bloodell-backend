package br.edu.cesar.bloodell.telemetria;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.alerta.infraestrutura.AlertaRepository;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.telemetria.aplicacao.TelemetriaService;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraEntrada;
import br.edu.cesar.bloodell.telemetria.infraestrutura.LeituraTelemetriaRepository;
import br.edu.cesar.bloodell.telemetria.infraestrutura.MetricasDeIngestao;
import br.edu.cesar.bloodell.telemetria.infraestrutura.ProcessadorDeTelemetria;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IngestaoDeTelemetriaIT extends TesteDeIntegracao {

    private static final int QUANTIDADE_DE_PRODUTORES = 8;
    private static final int LEITURAS_POR_PRODUTOR = 50;

    @Autowired
    private TelemetriaService telemetriaService;

    @Autowired
    private ProcessadorDeTelemetria processador;

    @Autowired
    private MetricasDeIngestao metricas;

    @Autowired
    private LeituraTelemetriaRepository leituras;

    @Autowired
    private AlertaRepository alertas;

    @Autowired
    private UnidadeRepository unidades;

    @Autowired
    private VeiculoRepository veiculos;

    @Test
    @DisplayName("400 leituras de 8 produtores simultaneos chegam todas ao banco")
    void ingestaoConcorrenteNaoPerdeLeitura() throws Exception {
        Unidade base = unidades.save(new Hemocentro("Hemocentro Telemetria " + System.nanoTime(),
                new Endereco("Rua", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.05, -34.88)));
        Veiculo veiculo = veiculos.save(new Veiculo("TLM-" + (System.nanoTime() % 10000), base, 20,
                new CoordenadaGeo(-8.05, -34.88)));

        long leiturasAntes = leituras.countByVeiculoId(veiculo.getId());
        metricas.zerar();

        AtomicInteger aceitas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(QUANTIDADE_DE_PRODUTORES);
        ExecutorService produtores = Executors.newFixedThreadPool(QUANTIDADE_DE_PRODUTORES);

        long inicio = System.currentTimeMillis();
        for (int produtor = 0; produtor < QUANTIDADE_DE_PRODUTORES; produtor++) {
            produtores.submit(() -> {
                try {
                    largada.await();
                    for (int indice = 0; indice < LEITURAS_POR_PRODUTOR; indice++) {
                        telemetriaService.receber(new LeituraEntrada(veiculo.getId(), null,
                                -8.05, -34.88, new BigDecimal("4.50"), "EM_ROTA",
                                OffsetDateTime.now()));
                        aceitas.incrementAndGet();
                    }
                } catch (InterruptedException interrupcao) {
                    Thread.currentThread().interrupt();
                } finally {
                    chegada.countDown();
                }
            });
        }

        largada.countDown();
        assertThat(chegada.await(60, TimeUnit.SECONDS)).isTrue();
        produtores.shutdown();

        assertThat(processador.aguardarFilaVazia(60_000))
                .as("os workers precisam esvaziar a fila")
                .isTrue();
        long duracaoMs = System.currentTimeMillis() - inicio;

        int esperado = QUANTIDADE_DE_PRODUTORES * LEITURAS_POR_PRODUTOR;
        assertThat(aceitas.get()).isEqualTo(esperado);
        assertThat(leituras.countByVeiculoId(veiculo.getId()) - leiturasAntes)
                .as("nenhuma leitura aceita pode se perder entre a fila e o banco")
                .isEqualTo(esperado);
        assertThat(metricas.getRecebidas()).isEqualTo(esperado);
        assertThat(metricas.getPersistidas()).isEqualTo(esperado);
        assertThat(metricas.getLotes()).isPositive();

        System.out.printf("[benchmark] %d leituras em %d ms (%.1f msg/s), %d lote(s), lote medio %d%n",
                esperado, duracaoMs, esperado * 1000.0 / Math.max(duracaoMs, 1),
                metricas.getLotes(), metricas.getTamanhoMedioDoLote());
    }

    @Test
    @DisplayName("temperatura fora da faixa didatica abre alerta de cadeia fria")
    void temperaturaForaDaFaixaGeraAlerta() throws Exception {
        Unidade base = unidades.save(new Hemocentro("Hemocentro Alerta " + System.nanoTime(),
                new Endereco("Rua", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.05, -34.88)));
        Veiculo veiculo = veiculos.save(new Veiculo("ALT-" + (System.nanoTime() % 10000), base, 20,
                new CoordenadaGeo(-8.05, -34.88)));

        int alertasAntes = alertas.listarAbertos().size();

        telemetriaService.receber(new LeituraEntrada(veiculo.getId(), null, -8.05, -34.88,
                new BigDecimal("12.30"), "EM_ROTA", OffsetDateTime.now()));

        assertThat(processador.aguardarFilaVazia(30_000)).isTrue();

        assertThat(alertas.listarAbertos().size())
                .as("12,3 C esta fora da faixa didatica de 2 a 6 C")
                .isGreaterThan(alertasAntes);
    }
}
