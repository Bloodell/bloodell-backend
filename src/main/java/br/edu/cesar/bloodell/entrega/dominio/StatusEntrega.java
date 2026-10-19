package br.edu.cesar.bloodell.entrega.dominio;

public enum StatusEntrega {

    PLANEJADA,
    DESPACHADA,
    EM_TRANSPORTE,
    ENTREGUE,
    CANCELADA;

    public boolean podeIrPara(StatusEntrega destino) {
        if (destino == null) {
            return false;
        }
        return switch (this) {
            case PLANEJADA -> destino == DESPACHADA || destino == CANCELADA;
            case DESPACHADA -> destino == EM_TRANSPORTE || destino == CANCELADA;
            case EM_TRANSPORTE -> destino == ENTREGUE || destino == CANCELADA;
            case ENTREGUE, CANCELADA -> false;
        };
    }
}
