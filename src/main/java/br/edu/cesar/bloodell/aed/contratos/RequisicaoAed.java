package br.edu.cesar.bloodell.aed.contratos;

public class RequisicaoAed {

    private final String codigo;
    private final String hospital;
    private final String prioridade;
    private final long sequenciaChegada;

    public RequisicaoAed(String codigo, String hospital, String prioridade, long sequenciaChegada) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Requisicao sem codigo nao entra na fila.");
        }
        this.codigo = codigo;
        this.hospital = hospital;
        this.prioridade = prioridade;
        this.sequenciaChegada = sequenciaChegada;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getHospital() {
        return hospital;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public long getSequenciaChegada() {
        return sequenciaChegada;
    }

    @Override
    public String toString() {
        return codigo + "|" + prioridade + "|" + sequenciaChegada;
    }
}
