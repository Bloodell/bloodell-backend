package br.edu.cesar.bloodell.aed.contratos;

public class EventoAed {

    private final String codigoBolsa;
    private final String statusAnterior;
    private final String statusNovo;
    private final String ocorridoEm;

    public EventoAed(String codigoBolsa, String statusAnterior, String statusNovo, String ocorridoEm) {
        if (codigoBolsa == null || codigoBolsa.isBlank()) {
            throw new IllegalArgumentException("Evento sem bolsa nao entra na pilha.");
        }
        this.codigoBolsa = codigoBolsa;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.ocorridoEm = ocorridoEm;
    }

    public String getCodigoBolsa() {
        return codigoBolsa;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public String getStatusNovo() {
        return statusNovo;
    }

    public String getOcorridoEm() {
        return ocorridoEm;
    }

    @Override
    public String toString() {
        return codigoBolsa + "|" + (statusAnterior == null ? "-" : statusAnterior) + "->" + statusNovo;
    }
}
