package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.EventoAed;

public class NoEvento {

    // A referencia abaixo corresponde ao ponteiro para o no inferior da pilha em C.
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
