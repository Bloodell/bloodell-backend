package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.EventoAed;

public class NoEvento {

    private final EventoAed dado;
    private NoEvento abaixo;

    public NoEvento(EventoAed dado) {
        this.dado = dado;
        this.abaixo = null;
    }

    public EventoAed getDado() {
        return dado;
    }

    public NoEvento getAbaixo() {
        return abaixo;
    }

    public void setAbaixo(NoEvento abaixo) {
        this.abaixo = abaixo;
    }
}
