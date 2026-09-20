package br.edu.cesar.bloodell.indicadores.apresentacao;

import br.edu.cesar.bloodell.indicadores.aplicacao.EstatisticaDescritiva;
import java.util.List;

public final class IndicadorDtos {

    private IndicadorDtos() {
    }

    public record ContagemPorCategoria(String categoria, long quantidade) {
    }

    public record IndicadorEstoqueResposta(
            String aviso,
            long totalDeBolsasDisponiveis,
            List<ContagemPorCategoria> porTipoSanguineo,
            List<ContagemPorCategoria> porComponente,
            EstatisticaDescritiva distribuicaoPorTipo) {
    }

    public record IndicadorDemandaResposta(
            String aviso,
            long totalDeRequisicoes,
            List<ContagemPorCategoria> porHospital,
            List<ContagemPorCategoria> porPrioridade,
            EstatisticaDescritiva distribuicaoPorHospital) {
    }

    public record IndicadorTemposResposta(
            String aviso,
            String unidade,
            long requisicoesAtendidas,
            long requisicoesNaFila,
            EstatisticaDescritiva tempoAteAtendimento) {
    }
}
