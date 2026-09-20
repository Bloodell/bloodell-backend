package br.edu.cesar.bloodell.demonstracao.apresentacao;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class DemonstracaoDtos {

    private DemonstracaoDtos() {
    }

    public record CenarioEntrada(
            @NotBlank(message = "informe o nome do cenario (ex.: aula)")
            String cenario,

            Long semente) {
    }

    public record CenarioResposta(
            String cenario,
            long semente,
            String aviso,
            int hemocentrosCriados,
            int hospitaisCriados,
            int veiculosCriados,
            int doacoesRegistradas,
            int bolsasGeradas,
            int requisicoesCriadas,
            List<String> proximosPassos) {
    }
}
