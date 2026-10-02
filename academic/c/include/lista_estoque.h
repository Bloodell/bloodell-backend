#ifndef BLOODELL_LISTA_ESTOQUE_H
#define BLOODELL_LISTA_ESTOQUE_H

#include "contratos.h"

typedef struct NoBolsa {
    Bolsa dado;
    struct NoBolsa *proximo;
} NoBolsa;

typedef struct {
    NoBolsa *cabeca;
    NoBolsa *cauda;
    int quantidade;
} ListaEstoque;

typedef void (*VisitarBolsa)(const Bolsa *bolsa, void *contexto);

void lista_estoque_inicializar(ListaEstoque *lista);
bool lista_estoque_inserir(ListaEstoque *lista, const Bolsa *bolsa);
bool lista_estoque_buscar_por_codigo(const ListaEstoque *lista, const char *codigo,
                                     Bolsa *encontrada);
bool lista_estoque_remover_por_codigo(ListaEstoque *lista, const char *codigo,
                                      Bolsa *removida);
bool lista_estoque_primeira_compativel_por_igualdade(
        const ListaEstoque *lista, const char *tipo_sanguineo,
        const char *componente, const char *status_exigido, Bolsa *escolhida);
void lista_estoque_listar(const ListaEstoque *lista, VisitarBolsa visitar, void *contexto);
int lista_estoque_tamanho(const ListaEstoque *lista);
bool lista_estoque_esta_vazia(const ListaEstoque *lista);
void lista_estoque_limpar(ListaEstoque *lista);

#endif
