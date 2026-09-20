package br.edu.cesar.bloodell.aed.contratos;

public class BolsaAed {

    private final String codigo;
    private final String tipoSanguineo;
    private final String componente;
    private final String dataValidade;
    private final long sequenciaEntrada;
    private final String status;

    public BolsaAed(String codigo, String tipoSanguineo, String componente,
                    String dataValidade, long sequenciaEntrada, String status) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Bolsa sem codigo nao entra na estrutura.");
        }
        this.codigo = codigo;
        this.tipoSanguineo = tipoSanguineo;
        this.componente = componente;
        this.dataValidade = dataValidade;
        this.sequenciaEntrada = sequenciaEntrada;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipoSanguineo() {
        return tipoSanguineo;
    }

    public String getComponente() {
        return componente;
    }

    public String getDataValidade() {
        return dataValidade;
    }

    public long getSequenciaEntrada() {
        return sequenciaEntrada;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return codigo + "|" + tipoSanguineo + "|" + componente + "|" + dataValidade;
    }
}
