package br.edu.cesar.bloodell.unidade.apresentacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UnidadeDtos {

    private UnidadeDtos() {
    }

    public record UnidadeRequisicao(
            @NotBlank(message = "informe o nome da unidade")
            @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
            String nome,

            String logradouro,
            String numero,
            String bairro,

            @NotBlank(message = "informe a cidade")
            String cidade,

            @NotBlank(message = "informe a UF")
            @Size(min = 2, max = 2, message = "UF deve ter 2 letras")
            String uf,

            String cep,

            @NotNull(message = "informe a latitude")
            @DecimalMin(value = "-90.0", message = "latitude minima e -90")
            @DecimalMax(value = "90.0", message = "latitude maxima e 90")
            Double latitude,

            @NotNull(message = "informe a longitude")
            @DecimalMin(value = "-180.0", message = "longitude minima e -180")
            @DecimalMax(value = "180.0", message = "longitude maxima e 180")
            Double longitude) {
    }

    public record UnidadeResposta(
            Long id,
            String tipo,
            String nome,
            String cidade,
            String uf,
            Double latitude,
            Double longitude) {
    }
}
