#include "pilha_historico.h"

#include <stdlib.h>

void pilha_historico_inicializar(PilhaHistorico *pilha) {
    if (pilha == NULL) {
        return;
    }
    pilha->topo = NULL;
    pilha->quantidade = 0;
}

bool pilha_historico_empilhar(PilhaHistorico *pilha, const Evento *evento) {
    NoEvento *novo;

    if (pilha == NULL || evento == NULL) {
        return false;
    }
    novo = malloc(sizeof(*novo));
    if (novo == NULL) {
        return false;
    }
    novo->dado = *evento;
    novo->abaixo = pilha->topo;
    pilha->topo = novo;
    pilha->quantidade++;
    return true;
}

bool pilha_historico_desempilhar(PilhaHistorico *pilha, Evento *retirado) {
    NoEvento *removido;

    if (pilha == NULL || retirado == NULL || pilha->topo == NULL) {
        return false;
    }
    removido = pilha->topo;
    pilha->topo = removido->abaixo;
    *retirado = removido->dado;
    free(removido);
    pilha->quantidade--;
    return true;
}

bool pilha_historico_topo(const PilhaHistorico *pilha, Evento *do_topo) {
    if (pilha == NULL || do_topo == NULL || pilha->topo == NULL) {
        return false;
    }
    *do_topo = pilha->topo->dado;
    return true;
}

void pilha_historico_listar(const PilhaHistorico *pilha,
                            VisitarEvento visitar, void *contexto) {
    NoEvento *atual;

    if (pilha == NULL || visitar == NULL) {
        return;
    }
    atual = pilha->topo;
    while (atual != NULL) {
        visitar(&atual->dado, contexto);
        atual = atual->abaixo;
    }
}

int pilha_historico_tamanho(const PilhaHistorico *pilha) {
    return pilha == NULL ? 0 : pilha->quantidade;
}

bool pilha_historico_esta_vazia(const PilhaHistorico *pilha) {
    return pilha == NULL || pilha->topo == NULL;
}

void pilha_historico_limpar(PilhaHistorico *pilha) {
    NoEvento *atual;
    NoEvento *abaixo;

    if (pilha == NULL) {
        return;
    }
    atual = pilha->topo;
    while (atual != NULL) {
        abaixo = atual->abaixo;
        free(atual);
        atual = abaixo;
    }
    pilha->topo = NULL;
    pilha->quantidade = 0;
}
