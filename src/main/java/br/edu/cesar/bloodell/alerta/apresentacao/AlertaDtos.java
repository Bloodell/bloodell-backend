package br.edu.cesar.bloodell.alerta.apresentacao;

import java.time.OffsetDateTime;

public final class AlertaDtos {

    private AlertaDtos() {
    }

    public record AlertaResposta(
            Long id,
            String tipo,
            String nivel,
            String mensagem,
            Long veiculoId,
            Long entregaId,
            Long bolsaId,
            OffsetDateTime criadoEm) {
    }
}
