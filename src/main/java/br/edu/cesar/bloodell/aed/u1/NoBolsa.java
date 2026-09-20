package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;

public class NoBolsa {

    private final BolsaAed dado;
    private NoBolsa proximo;

    public NoBolsa(BolsaAed dado) {
        this.dado = dado;
        this.proximo = null;
    }

    public BolsaAed getDado() {
        return dado;
    }

    public NoBolsa getProximo() {
        return proximo;
    }

    public void setProximo(NoBolsa proximo) {
        this.proximo = proximo;
    }
}
