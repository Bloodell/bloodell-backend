package br.edu.cesar.bloodell.telemetria.infraestrutura;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class MetricasDeIngestao {

    private final AtomicLong recebidas = new AtomicLong();
    private final AtomicLong persistidas = new AtomicLong();
    private final AtomicLong recusadas = new AtomicLong();
    private final AtomicLong lotes = new AtomicLong();
    private final AtomicLong tempoDeGravacaoMs = new AtomicLong();

    public void contarRecebida() {
        recebidas.incrementAndGet();
    }

    public void contarRecusada() {
        recusadas.incrementAndGet();
    }

    public void contarLote(int tamanho, long duracaoMs) {
        lotes.incrementAndGet();
        persistidas.addAndGet(tamanho);
        tempoDeGravacaoMs.addAndGet(duracaoMs);
    }

    public long getRecebidas() {
        return recebidas.get();
    }

    public long getPersistidas() {
        return persistidas.get();
    }

    public long getRecusadas() {
        return recusadas.get();
    }

    public long getLotes() {
        return lotes.get();
    }

    public long getTempoDeGravacaoMs() {
        return tempoDeGravacaoMs.get();
    }

    public long getTamanhoMedioDoLote() {
        long quantidade = lotes.get();
        return quantidade == 0 ? 0 : persistidas.get() / quantidade;
    }

    public void zerar() {
        recebidas.set(0);
        persistidas.set(0);
        recusadas.set(0);
        lotes.set(0);
        tempoDeGravacaoMs.set(0);
    }
}
