#include "fila_requisicoes.h"

#include <stdlib.h>
#include <string.h>

static const char *PRIORIDADES[QUANTIDADE_DE_PRIORIDADES] = {
    "EMERGENCIA", "URGENTE", "ELETIVA"
};

static int indice_da_prioridade(const char *prioridade) {
    int faixa;

    if (prioridade == NULL) {
        return -1;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        if (strcmp(PRIORIDADES[faixa], prioridade) == 0) {
            return faixa;
        }
    }
    return -1;
}

void fila_requisicoes_inicializar(FilaRequisicoes *fila) {
    int faixa;

    if (fila == NULL) {
        return;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        fila->cabecas[faixa] = NULL;
        fila->caudas[faixa] = NULL;
    }
    fila->quantidade = 0;
}

bool fila_requisicoes_enfileirar(FilaRequisicoes *fila, const Requisicao *requisicao) {
    NoRequisicao *novo;
    int faixa;

    if (fila == NULL || requisicao == NULL) {
        return false;
    }
    faixa = indice_da_prioridade(requisicao->prioridade);
    if (faixa < 0) {
        return false;
    }

    novo = malloc(sizeof(*novo));
    if (novo == NULL) {
        return false;
    }
    novo->dado = *requisicao;
    novo->proximo = NULL;

    if (fila->cabecas[faixa] == NULL) {
        fila->cabecas[faixa] = novo;
    } else {
        fila->caudas[faixa]->proximo = novo;
    }
    fila->caudas[faixa] = novo;
    fila->quantidade++;
    return true;
}

bool fila_requisicoes_desenfileirar(FilaRequisicoes *fila, Requisicao *retirada) {
    int faixa;
    NoRequisicao *removido;

    if (fila == NULL || retirada == NULL) {
        return false;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        if (fila->cabecas[faixa] != NULL) {
            removido = fila->cabecas[faixa];
            fila->cabecas[faixa] = removido->proximo;
            if (fila->cabecas[faixa] == NULL) {
                fila->caudas[faixa] = NULL;
            }
            *retirada = removido->dado;
            free(removido);
            fila->quantidade--;
            return true;
        }
    }
    return false;
}

bool fila_requisicoes_espiar(const FilaRequisicoes *fila, Requisicao *proxima) {
    int faixa;

    if (fila == NULL || proxima == NULL) {
        return false;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        if (fila->cabecas[faixa] != NULL) {
            *proxima = fila->cabecas[faixa]->dado;
            return true;
        }
    }
    return false;
}

void fila_requisicoes_listar(const FilaRequisicoes *fila,
                             VisitarRequisicao visitar, void *contexto) {
    int faixa;
    NoRequisicao *atual;

    if (fila == NULL || visitar == NULL) {
        return;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        atual = fila->cabecas[faixa];
        while (atual != NULL) {
            visitar(&atual->dado, contexto);
            atual = atual->proximo;
        }
    }
}

int fila_requisicoes_tamanho(const FilaRequisicoes *fila) {
    return fila == NULL ? 0 : fila->quantidade;
}

bool fila_requisicoes_esta_vazia(const FilaRequisicoes *fila) {
    return fila == NULL || fila->quantidade == 0;
}

void fila_requisicoes_limpar(FilaRequisicoes *fila) {
    int faixa;
    NoRequisicao *atual;
    NoRequisicao *proximo;

    if (fila == NULL) {
        return;
    }
    for (faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        atual = fila->cabecas[faixa];
        while (atual != NULL) {
            proximo = atual->proximo;
            free(atual);
            atual = proximo;
        }
        fila->cabecas[faixa] = NULL;
        fila->caudas[faixa] = NULL;
    }
    fila->quantidade = 0;
}
