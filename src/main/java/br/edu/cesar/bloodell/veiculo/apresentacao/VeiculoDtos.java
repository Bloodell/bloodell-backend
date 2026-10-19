package br.edu.cesar.bloodell.veiculo.apresentacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VeiculoDtos {

    private VeiculoDtos() {
    }

    public record VeiculoRequisicao(
            @NotBlank(message = "informe a placa")
            @Size(max = 10, message = "placa deve ter no maximo 10 caracteres")
            String placa,

            @NotNull(message = "informe o hemocentro base")
            Long hemocentroBaseId,

            @NotNull(message = "informe a capacidade em bolsas")
            @Min(value = 1, message = "capacidade deve ser maior que zero")
            Integer capacidadeBolsas,

            @NotNull(message = "informe a latitude")
            @DecimalMin(value = "-90.0", message = "latitude minima e -90")
            @DecimalMax(value = "90.0", message = "latitude maxima e 90")
            Double latitude,

            @NotNull(message = "informe a longitude")
            @DecimalMin(value = "-180.0", message = "longitude minima e -180")
            @DecimalMax(value = "180.0", message = "longitude maxima e 180")
            Double longitude) {
    }

    public record VeiculoResposta(
            Long id,
            String placa,
            Long hemocentroBaseId,
            String hemocentroBaseNome,
            Integer capacidadeBolsas,
            String status,
            Double latitude,
            Double longitude) {
    }
}
