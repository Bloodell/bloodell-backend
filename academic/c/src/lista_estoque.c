#include "lista_estoque.h"

#include <stdlib.h>
#include <string.h>

void lista_estoque_inicializar(ListaEstoque *lista) {
    if (lista == NULL) {
        return;
    }
    lista->cabeca = NULL;
    lista->cauda = NULL;
    lista->quantidade = 0;
}

bool lista_estoque_inserir(ListaEstoque *lista, const Bolsa *bolsa) {
    NoBolsa *novo;

    if (lista == NULL || bolsa == NULL) {
        return false;
    }

    novo = malloc(sizeof(*novo));
    if (novo == NULL) {
        return false;
    }
    novo->dado = *bolsa;
    novo->proximo = NULL;

    if (lista->cabeca == NULL) {
        lista->cabeca = novo;
    } else {
        lista->cauda->proximo = novo;
    }
    lista->cauda = novo;
    lista->quantidade++;
    return true;
}

bool lista_estoque_buscar_por_codigo(const ListaEstoque *lista, const char *codigo,
                                     Bolsa *encontrada) {
    NoBolsa *atual;

    if (lista == NULL || codigo == NULL || encontrada == NULL) {
        return false;
    }
    atual = lista->cabeca;
    while (atual != NULL) {
        if (strcmp(atual->dado.codigo, codigo) == 0) {
            *encontrada = atual->dado;
            return true;
        }
        atual = atual->proximo;
    }
    return false;
}

bool lista_estoque_remover_por_codigo(ListaEstoque *lista, const char *codigo,
                                      Bolsa *removida) {
    NoBolsa *anterior = NULL;
    NoBolsa *atual;

    if (lista == NULL || codigo == NULL || removida == NULL) {
        return false;
    }
    atual = lista->cabeca;
    while (atual != NULL) {
        if (strcmp(atual->dado.codigo, codigo) == 0) {
            if (anterior == NULL) {
                lista->cabeca = atual->proximo;
            } else {
                anterior->proximo = atual->proximo;
            }
            if (atual == lista->cauda) {
                lista->cauda = anterior;
            }
            *removida = atual->dado;
            free(atual);
            lista->quantidade--;
            if (lista->cabeca == NULL) {
                lista->cauda = NULL;
            }
            return true;
        }
        anterior = atual;
        atual = atual->proximo;
    }
    return false;
}

bool lista_estoque_primeira_compativel_por_igualdade(
        const ListaEstoque *lista, const char *tipo_sanguineo,
        const char *componente, const char *status_exigido, Bolsa *escolhida) {
    NoBolsa *atual;

    if (lista == NULL || tipo_sanguineo == NULL || componente == NULL
            || escolhida == NULL) {
        return false;
    }
    atual = lista->cabeca;
    while (atual != NULL) {
        bool mesmo_tipo = strcmp(atual->dado.tipo_sanguineo, tipo_sanguineo) == 0;
        bool mesmo_componente = strcmp(atual->dado.componente, componente) == 0;
        bool status_ok = status_exigido == NULL
                || strcmp(atual->dado.status, status_exigido) == 0;
        if (mesmo_tipo && mesmo_componente && status_ok) {
            *escolhida = atual->dado;
            return true;
        }
        atual = atual->proximo;
    }
    return false;
}

void lista_estoque_listar(const ListaEstoque *lista, VisitarBolsa visitar, void *contexto) {
    NoBolsa *atual;

    if (lista == NULL || visitar == NULL) {
        return;
    }
    atual = lista->cabeca;
    while (atual != NULL) {
        visitar(&atual->dado, contexto);
        atual = atual->proximo;
    }
}

int lista_estoque_tamanho(const ListaEstoque *lista) {
    return lista == NULL ? 0 : lista->quantidade;
}

bool lista_estoque_esta_vazia(const ListaEstoque *lista) {
    return lista == NULL || lista->cabeca == NULL;
}

void lista_estoque_limpar(ListaEstoque *lista) {
    NoBolsa *atual;
    NoBolsa *proximo;

    if (lista == NULL) {
        return;
    }
    atual = lista->cabeca;
    while (atual != NULL) {
        proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }
    lista->cabeca = NULL;
    lista->cauda = NULL;
    lista->quantidade = 0;
}
