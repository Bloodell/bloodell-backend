package br.edu.cesar.bloodell.bolsa.dominio;

public enum EstadoBolsa {

    RECEBIDA,
    ARMAZENADA,
    DISPONIVEL,
    RESERVADA,
    ALOCADA,
    DESPACHADA,
    EM_TRANSPORTE,
    ENTREGUE,
    DESCARTADA,
    VENCIDA;

    static {
        RECEBIDA.seguintes = new EstadoBolsa[]{ARMAZENADA, DESCARTADA};
        ARMAZENADA.seguintes = new EstadoBolsa[]{DISPONIVEL, DESCARTADA, VENCIDA};
        DISPONIVEL.seguintes = new EstadoBolsa[]{RESERVADA, DESCARTADA, VENCIDA};
        RESERVADA.seguintes = new EstadoBolsa[]{ALOCADA, DISPONIVEL, DESCARTADA};
        ALOCADA.seguintes = new EstadoBolsa[]{DESPACHADA, DISPONIVEL, DESCARTADA};
        DESPACHADA.seguintes = new EstadoBolsa[]{EM_TRANSPORTE, DESCARTADA};
        EM_TRANSPORTE.seguintes = new EstadoBolsa[]{ENTREGUE, DESCARTADA};
        ENTREGUE.seguintes = new EstadoBolsa[]{};
        DESCARTADA.seguintes = new EstadoBolsa[]{};
        VENCIDA.seguintes = new EstadoBolsa[]{DESCARTADA};
    }

    private EstadoBolsa[] seguintes;

    public boolean podeIrPara(EstadoBolsa destino) {
        if (destino == null) {
            return false;
        }
        for (EstadoBolsa candidato : seguintes) {
            if (candidato == destino) {
                return true;
            }
        }
        return false;
    }

    public boolean disponivelParaAlocacao() {
        return this == DISPONIVEL;
    }
}
