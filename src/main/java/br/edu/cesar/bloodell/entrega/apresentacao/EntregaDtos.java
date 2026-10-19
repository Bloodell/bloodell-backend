package br.edu.cesar.bloodell.entrega.apresentacao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class EntregaDtos {

    private EntregaDtos() {
    }

    public record EntregaEntrada(
            @NotNull(message = "informe a requisicao") Long requisicaoId,
            @NotNull(message = "informe o veiculo") Long veiculoId,
            @NotNull(message = "informe a unidade de origem") Long origemId,
            String rota,
            @DecimalMin(value = "0.0", inclusive = true, message = "distancia nao pode ser negativa") BigDecimal distanciaKm,
            @Positive(message = "tempo estimado deve ser maior que zero") Integer tempoEstimadoMin) {
    }

    public record PlanejamentoEntregaEntrada(
            String rota,
            @DecimalMin(value = "0.0", inclusive = true, message = "distancia nao pode ser negativa") BigDecimal distanciaKm,
            @Positive(message = "tempo estimado deve ser maior que zero") Integer tempoEstimadoMin) {
    }

    public record EntregaResposta(
            Long id,
            Long requisicaoId,
            String requisicaoCodigo,
            Long veiculoId,
            String veiculoPlaca,
            Long origemId,
            String origemNome,
            String status,
            String rota,
            BigDecimal distanciaKm,
            Integer tempoEstimadoMin,
            OffsetDateTime iniciadaEm,
            OffsetDateTime concluidaEm) {
    }

    public record RotaPlanejada(List<ParadaRota> paradas) {
    }

    public record ParadaRota(int ordem, String tipo, Long unidadeId, String nome,
                             double latitude, double longitude) {
    }
}
