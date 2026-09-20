package br.edu.cesar.bloodell.requisicao.apresentacao;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;

public final class RequisicaoDtos {

    private RequisicaoDtos() {
    }

    public record ItemRequisicaoEntrada(
            @NotNull(message = "informe o componente")
            TipoComponente componente,

            @NotNull(message = "informe o tipo sanguineo solicitado")
            @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "tipo sanguineo deve ser A, B, AB ou O seguido de + ou -")
            String tipoSanguineo,

            @NotNull(message = "informe a quantidade")
            @Min(value = 1, message = "quantidade deve ser pelo menos 1")
            Integer quantidade) {
    }

    public record RequisicaoEntrada(
            @NotNull(message = "informe o hospital solicitante")
            Long hospitalId,

            @NotNull(message = "informe a prioridade")
            Prioridade prioridade,

            @NotEmpty(message = "a requisicao precisa de ao menos um item")
            List<@Valid ItemRequisicaoEntrada> itens) {
    }

    public record ItemRequisicaoResposta(
            Long id,
            String componente,
            String tipoSanguineo,
            Integer quantidade,
            Integer quantidadeAlocada) {
    }

    public record RequisicaoResposta(
            Long id,
            String codigo,
            Long hospitalId,
            String hospitalNome,
            String prioridade,
            String status,
            OffsetDateTime criadaEm,
            OffsetDateTime atendidaEm,
            List<ItemRequisicaoResposta> itens) {
    }

    public record ItemDaFilaResposta(
            int posicao,
            String codigo,
            String hospital,
            String prioridade,
            long sequenciaChegada) {
    }
}
