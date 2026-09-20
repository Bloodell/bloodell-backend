package br.edu.cesar.bloodell.veiculo.apresentacao;

public final class VeiculoDtos {

    private VeiculoDtos() {
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
