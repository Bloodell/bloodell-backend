package br.edu.cesar.bloodell.telemetria.apresentacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class TelemetriaDtos {

    private TelemetriaDtos() {
    }

    public record LeituraEntrada(
            @NotNull(message = "informe o veiculo")
            Long veiculoId,

            Long entregaId,

            @NotNull(message = "informe a latitude")
            @DecimalMin(value = "-90.0", message = "latitude minima e -90")
            @DecimalMax(value = "90.0", message = "latitude maxima e 90")
            Double lat,

            @NotNull(message = "informe a longitude")
            @DecimalMin(value = "-180.0", message = "longitude minima e -180")
            @DecimalMax(value = "180.0", message = "longitude maxima e 180")
            Double lon,

            @NotNull(message = "informe a temperatura em graus Celsius")
            @DecimalMin(value = "-40.0", message = "temperatura abaixo do intervalo plausivel")
            @DecimalMax(value = "60.0", message = "temperatura acima do intervalo plausivel")
            BigDecimal temperaturaC,

            @NotBlank(message = "informe o status do veiculo")
            String statusVeiculo,

            OffsetDateTime registradaEm) {
    }

    public record AceiteResposta(
            String correlacao,
            int naFila,
            String observacao) {
    }

    public record LeituraResposta(
            Long id,
            Long veiculoId,
            Double lat,
            Double lon,
            BigDecimal temperaturaC,
            String statusVeiculo,
            OffsetDateTime registradaEm,
            OffsetDateTime recebidaEm,
            boolean dentroDaFaixaDidatica) {
    }

    public record MetricasResposta(
            long leiturasRecebidas,
            long leiturasPersistidas,
            long leiturasRecusadas,
            long lotesGravados,
            long tamanhoMedioDoLote,
            long tempoTotalDeGravacaoMs,
            int tamanhoAtualDaFila,
            int capacidadeDaFila,
            int quantidadeDeWorkers) {
    }
}
