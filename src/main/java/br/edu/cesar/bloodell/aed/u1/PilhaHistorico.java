package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.EventoAed;

public class PilhaHistorico {

    private NoEvento topo;
    private int quantidade;

    public PilhaHistorico() {
        this.topo = null;
        this.quantidade = 0;
    }

    // NoEvento.abaixo equivale ao ponteiro abaixo em C; topo null equivale a topo == NULL.
    // C libera cada no com free; Java apenas rompe as referencias e deixa a coleta cuidar do objeto.
    public void empilhar(EventoAed evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Nao e possivel empilhar evento nulo.");
        }
        NoEvento novo = new NoEvento(evento);
        novo.setAbaixo(topo);
        topo = novo;
        quantidade++;
    }

    public EventoAed desempilhar() {
        if (topo == null) {
            return null;
        }
        NoEvento removido = topo;
        topo = removido.getAbaixo();
        removido.setAbaixo(null);
        quantidade--;
        return removido.getDado();
    }

    public EventoAed topo() {
        return topo == null ? null : topo.getDado();
    }

    public EventoAed[] listar() {
        EventoAed[] itens = new EventoAed[quantidade];
        NoEvento atual = topo;
        int indice = 0;
        while (atual != null) {
            itens[indice] = atual.getDado();
            indice++;
            atual = atual.getAbaixo();
        }
        return itens;
    }

    public EventoAed[] paraVetor() {
        return listar();
    }

    public int tamanho() {
        return quantidade;
    }

    public boolean estaVazia() {
        return topo == null;
    }

    public void limpar() {
        NoEvento atual = topo;
        while (atual != null) {
            NoEvento abaixo = atual.getAbaixo();
            atual.setAbaixo(null);
            atual = abaixo;
        }
        topo = null;
        quantidade = 0;
    }
}
