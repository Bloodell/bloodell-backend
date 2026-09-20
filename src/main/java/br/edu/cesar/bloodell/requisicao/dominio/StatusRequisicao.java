package br.edu.cesar.bloodell.requisicao.dominio;

public enum StatusRequisicao {

    CRIADA,
    NA_FILA,
    EM_PROCESSAMENTO,
    ALOCADA,
    EM_ROTA,
    ATENDIDA,
    REJEITADA,
    CANCELADA;

    static {
        CRIADA.seguintes = new StatusRequisicao[]{NA_FILA, CANCELADA};
        NA_FILA.seguintes = new StatusRequisicao[]{EM_PROCESSAMENTO, CANCELADA};
        EM_PROCESSAMENTO.seguintes = new StatusRequisicao[]{ALOCADA, REJEITADA, NA_FILA};
        ALOCADA.seguintes = new StatusRequisicao[]{EM_ROTA, CANCELADA};
        EM_ROTA.seguintes = new StatusRequisicao[]{ATENDIDA, CANCELADA};
        ATENDIDA.seguintes = new StatusRequisicao[]{};
        REJEITADA.seguintes = new StatusRequisicao[]{NA_FILA};
        CANCELADA.seguintes = new StatusRequisicao[]{};
    }

    private StatusRequisicao[] seguintes;

    public boolean podeIrPara(StatusRequisicao destino) {
        if (destino == null) {
            return false;
        }
        for (StatusRequisicao candidato : seguintes) {
            if (candidato == destino) {
                return true;
            }
        }
        return false;
    }
}
