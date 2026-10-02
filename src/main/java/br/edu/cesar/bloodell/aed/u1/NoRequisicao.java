package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;

public class NoRequisicao {

    // A referencia proximo ocupa o papel do ponteiro struct NoRequisicao* em C.
    private final RequisicaoAed dado;
    private NoRequisicao proximo;

    public NoRequisicao(RequisicaoAed dado) {
        this.dado = dado;
        this.proximo = null;
    }

    public RequisicaoAed getDado() {
        return dado;
    }

    public NoRequisicao getProximo() {
        return proximo;
    }

    public void setProximo(NoRequisicao proximo) {
        this.proximo = proximo;
    }
}
