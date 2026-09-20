package br.edu.cesar.bloodell.bolsa.apresentacao;

import java.time.OffsetDateTime;

public final class BolsaDtos {

    private BolsaDtos() {
    }

    public record BolsaResposta(
            Long id,
            String codigo,
            String tipoSanguineo,
            String componente,
            String dataColeta,
            String dataValidade,
            String status,
            Long unidadeAtualId,
            String unidadeAtualNome,
            long sequenciaEntrada,
            boolean dentroDaValidade,
            int diasParaVencer) {
    }

    public record EventoResposta(
            Long id,
            String statusAnterior,
            String statusNovo,
            String descricao,
            OffsetDateTime ocorridoEm) {
    }
}
