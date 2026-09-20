package br.edu.cesar.bloodell.doacao.apresentacao;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.List;

public final class DoacaoDtos {

    private DoacaoDtos() {
    }

    public record DoacaoRequisicao(
            @NotNull(message = "informe o hemocentro")
            Long hemocentroId,

            @NotNull(message = "informe o tipo sanguineo, no formato A+, O-, AB+")
            @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "tipo sanguineo deve ser A, B, AB ou O seguido de + ou -")
            String tipoSanguineo,

            @NotNull(message = "informe o volume em ml")
            @Min(value = 100, message = "volume minimo de 100 ml")
            @Max(value = 1000, message = "volume maximo de 1000 ml")
            Integer volumeMl,

            OffsetDateTime dataHora,

            @NotEmpty(message = "informe ao menos um componente a ser gerado")
            List<TipoComponente> componentes) {
    }

    public record BolsaGeradaResposta(
            Long id,
            String codigo,
            String componente,
            String dataValidade,
            String status) {
    }

    public record DoacaoResposta(
            Long id,
            String codigo,
            Long hemocentroId,
            String tipoSanguineo,
            Integer volumeMl,
            OffsetDateTime dataHora,
            List<BolsaGeradaResposta> bolsasGeradas) {
    }
}
