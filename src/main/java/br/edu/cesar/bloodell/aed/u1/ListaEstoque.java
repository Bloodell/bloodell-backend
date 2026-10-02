package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;

public class ListaEstoque {

    private NoBolsa cabeca;
    private NoBolsa cauda;
    private int quantidade;

    public ListaEstoque() {
        this.cabeca = null;
        this.cauda = null;
        this.quantidade = 0;
    }

    // Em C, NoBolsa.proximo e um ponteiro para struct e cabeca NULL encerra a lista.
    // Aqui a referencia equivalente e null; C usa malloc/free, Java cria o no e o GC o recolhe.
    public void inserir(BolsaAed bolsa) {
        if (bolsa == null) {
            throw new IllegalArgumentException("Nao e possivel inserir bolsa nula na lista.");
        }
        NoBolsa novo = new NoBolsa(bolsa);
        if (cabeca == null) {
            cabeca = novo;
            cauda = novo;
        } else {
            cauda.setProximo(novo);
            cauda = novo;
        }
        quantidade++;
    }

    public BolsaAed buscarPorCodigo(String codigo) {
        NoBolsa atual = cabeca;
        while (atual != null) {
            if (atual.getDado().getCodigo().equals(codigo)) {
                return atual.getDado();
            }
            atual = atual.getProximo();
        }
        return null;
    }

    public BolsaAed removerPorCodigo(String codigo) {
        NoBolsa anterior = null;
        NoBolsa atual = cabeca;
        while (atual != null) {
            if (atual.getDado().getCodigo().equals(codigo)) {
                if (anterior == null) {
                    cabeca = atual.getProximo();
                } else {
                    anterior.setProximo(atual.getProximo());
                }
                if (atual == cauda) {
                    cauda = anterior;
                }
                atual.setProximo(null);
                quantidade--;
                return atual.getDado();
            }
            anterior = atual;
            atual = atual.getProximo();
        }
        return null;
    }

    public BolsaAed primeiraCompativelPorIgualdade(String tipoSanguineo, String componente, String statusExigido) {
        NoBolsa atual = cabeca;
        while (atual != null) {
            BolsaAed bolsa = atual.getDado();
            boolean mesmoTipo = bolsa.getTipoSanguineo().equals(tipoSanguineo);
            boolean mesmoComponente = bolsa.getComponente().equals(componente);
            boolean statusOk = statusExigido == null || statusExigido.equals(bolsa.getStatus());
            if (mesmoTipo && mesmoComponente && statusOk) {
                return bolsa;
            }
            atual = atual.getProximo();
        }
        return null;
    }

    public BolsaAed[] listar() {
        BolsaAed[] itens = new BolsaAed[quantidade];
        NoBolsa atual = cabeca;
        int indice = 0;
        while (atual != null) {
            itens[indice] = atual.getDado();
            indice++;
            atual = atual.getProximo();
        }
        return itens;
    }

    /** Alias usado pelo backend para deixar explicita a operacao de listagem. */
    public BolsaAed[] paraVetor() {
        return listar();
    }

    public int tamanho() {
        return quantidade;
    }

    public boolean estaVazia() {
        return cabeca == null;
    }

    public void limpar() {
        NoBolsa atual = cabeca;
        while (atual != null) {
            NoBolsa proximo = atual.getProximo();
            atual.setProximo(null);
            atual = proximo;
        }
        cabeca = null;
        cauda = null;
        quantidade = 0;
    }
}
