#ifndef BLOODELL_FILA_REQUISICOES_H
#define BLOODELL_FILA_REQUISICOES_H

#include "contratos.h"

#define QUANTIDADE_DE_PRIORIDADES 3

typedef struct NoRequisicao {
    Requisicao dado;
    struct NoRequisicao *proximo;
} NoRequisicao;

typedef struct {
    NoRequisicao *cabecas[QUANTIDADE_DE_PRIORIDADES];
    NoRequisicao *caudas[QUANTIDADE_DE_PRIORIDADES];
    int quantidade;
} FilaRequisicoes;

typedef void (*VisitarRequisicao)(const Requisicao *requisicao, void *contexto);

void fila_requisicoes_inicializar(FilaRequisicoes *fila);
bool fila_requisicoes_enfileirar(FilaRequisicoes *fila, const Requisicao *requisicao);
bool fila_requisicoes_desenfileirar(FilaRequisicoes *fila, Requisicao *retirada);
bool fila_requisicoes_espiar(const FilaRequisicoes *fila, Requisicao *proxima);
void fila_requisicoes_listar(const FilaRequisicoes *fila,
                             VisitarRequisicao visitar, void *contexto);
int fila_requisicoes_tamanho(const FilaRequisicoes *fila);
bool fila_requisicoes_esta_vazia(const FilaRequisicoes *fila);
void fila_requisicoes_limpar(FilaRequisicoes *fila);

#endif
