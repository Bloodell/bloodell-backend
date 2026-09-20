package br.edu.cesar.bloodell.alocacao.apresentacao;

import java.util.List;

public final class AlocacaoDtos {

    private AlocacaoDtos() {
    }

    public record PassoDaDecisao(
            int ordem,
            String etapa,
            String detalhe,
            String resultado) {
    }

    public record BolsaAlocadaResposta(
            Long bolsaId,
            String codigo,
            String tipoSanguineo,
            String componente,
            String dataValidade,
            long sequenciaEntrada) {
    }

    public record ResultadoAlocacaoResposta(
            Long requisicaoId,
            String requisicaoCodigo,
            String statusRequisicao,
            String politicaAplicada,
            String avisoAcademico,
            int quantidadeSolicitada,
            int quantidadeAlocada,
            List<BolsaAlocadaResposta> bolsas,
            List<PassoDaDecisao> passos) {
    }
}
