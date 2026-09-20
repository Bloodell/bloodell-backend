package br.edu.cesar.bloodell.estoque.apresentacao;

import java.time.OffsetDateTime;

public final class EstoqueDtos {

    private EstoqueDtos() {
    }

    public record EstoqueItemResposta(
            Long id,
            Long unidadeId,
            String unidadeNome,
            String tipoSanguineo,
            String componente,
            Integer quantidade,
            OffsetDateTime atualizadoEm) {
    }
}
