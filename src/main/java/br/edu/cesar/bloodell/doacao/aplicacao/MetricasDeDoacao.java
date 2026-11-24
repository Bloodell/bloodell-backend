package br.edu.cesar.bloodell.doacao.aplicacao;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MetricasDeDoacao {
    public record DoacaoRegistrada(int bolsasGeradas) { }

    private final Counter doacoes;
    private final Counter bolsas;

    public MetricasDeDoacao(MeterRegistry registry) {
        doacoes = Counter.builder("bloodell.doacoes.registradas")
                .description("Doações confirmadas no banco").register(registry);
        bolsas = Counter.builder("bloodell.bolsas.geradas")
                .description("Bolsas geradas por doações confirmadas").register(registry);
    }

    @TransactionalEventListener
    public void contabilizar(DoacaoRegistrada evento) {
        doacoes.increment();
        bolsas.increment(evento.bolsasGeradas());
    }
}
