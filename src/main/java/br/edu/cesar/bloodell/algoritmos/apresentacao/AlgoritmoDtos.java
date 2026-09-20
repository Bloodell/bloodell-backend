package br.edu.cesar.bloodell.algoritmos.apresentacao;

import java.time.OffsetDateTime;
import java.util.List;

public final class AlgoritmoDtos {

    private AlgoritmoDtos() {
    }

    public record NoDaLista(int posicao, String codigo, String tipoSanguineo,
                            String componente, String dataValidade, String status,
                            long sequenciaEntrada) {
    }

    public record SnapshotDaLista(String estrutura, String ordem, int tamanho,
                                  OffsetDateTime capturadoEm, String observacao,
                                  List<NoDaLista> nos) {
    }

    public record NoDaFila(int posicao, String codigo, String hospital,
                           String prioridade, long sequenciaChegada) {
    }

    public record SnapshotDaFila(String estrutura, String politica, int tamanho,
                                 OffsetDateTime capturadoEm, String observacao,
                                 List<NoDaFila> nos) {
    }

    public record NoDaPilha(int posicaoDoTopo, String codigoBolsa, String statusAnterior,
                            String statusNovo, String ocorridoEm) {
    }

    public record SnapshotDaPilha(String estrutura, String ordem, int tamanho,
                                  OffsetDateTime capturadoEm, String observacao,
                                  List<NoDaPilha> nos) {
    }

    public record SituacaoDoModuloAed(
            String etapaVigente,
            List<String> implementados,
            List<String> pendentesDaUnidadeDois,
            String observacao) {
    }
}
