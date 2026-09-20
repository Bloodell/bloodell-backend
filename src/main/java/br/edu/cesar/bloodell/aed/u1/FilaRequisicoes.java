package br.edu.cesar.bloodell.aed.u1;

import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;

public class FilaRequisicoes {

    private static final String[] PRIORIDADES = {"EMERGENCIA", "URGENTE", "ELETIVA"};

    private final NoRequisicao[] cabecas = new NoRequisicao[PRIORIDADES.length];
    private final NoRequisicao[] caudas = new NoRequisicao[PRIORIDADES.length];
    private int quantidade;

    public FilaRequisicoes() {
        this.quantidade = 0;
    }

    public void enfileirar(RequisicaoAed requisicao) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Nao e possivel enfileirar requisicao nula.");
        }
        int faixa = indiceDaPrioridade(requisicao.getPrioridade());
        NoRequisicao novo = new NoRequisicao(requisicao);
        if (cabecas[faixa] == null) {
            cabecas[faixa] = novo;
            caudas[faixa] = novo;
        } else {
            caudas[faixa].setProximo(novo);
            caudas[faixa] = novo;
        }
        quantidade++;
    }

    public RequisicaoAed desenfileirar() {
        for (int faixa = 0; faixa < PRIORIDADES.length; faixa++) {
            NoRequisicao cabeca = cabecas[faixa];
            if (cabeca != null) {
                cabecas[faixa] = cabeca.getProximo();
                if (cabecas[faixa] == null) {
                    caudas[faixa] = null;
                }
                cabeca.setProximo(null);
                quantidade--;
                return cabeca.getDado();
            }
        }
        return null;
    }

    public RequisicaoAed espiar() {
        for (int faixa = 0; faixa < PRIORIDADES.length; faixa++) {
            if (cabecas[faixa] != null) {
                return cabecas[faixa].getDado();
            }
        }
        return null;
    }

    public RequisicaoAed[] paraVetor() {
        RequisicaoAed[] itens = new RequisicaoAed[quantidade];
        int indice = 0;
        for (int faixa = 0; faixa < PRIORIDADES.length; faixa++) {
            NoRequisicao atual = cabecas[faixa];
            while (atual != null) {
                itens[indice] = atual.getDado();
                indice++;
                atual = atual.getProximo();
            }
        }
        return itens;
    }

    public int tamanho() {
        return quantidade;
    }

    public boolean estaVazia() {
        return quantidade == 0;
    }

    public void limpar() {
        for (int faixa = 0; faixa < PRIORIDADES.length; faixa++) {
            cabecas[faixa] = null;
            caudas[faixa] = null;
        }
        quantidade = 0;
    }

    private int indiceDaPrioridade(String prioridade) {
        for (int faixa = 0; faixa < PRIORIDADES.length; faixa++) {
            if (PRIORIDADES[faixa].equals(prioridade)) {
                return faixa;
            }
        }
        throw new IllegalArgumentException("Prioridade desconhecida na fila: " + prioridade);
    }
}