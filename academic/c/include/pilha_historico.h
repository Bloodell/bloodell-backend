#ifndef BLOODELL_PILHA_HISTORICO_H
#define BLOODELL_PILHA_HISTORICO_H

#include "contratos.h"

typedef struct NoEvento {
    Evento dado;
    struct NoEvento *abaixo;
} NoEvento;

typedef struct {
    NoEvento *topo;
    int quantidade;
} PilhaHistorico;

typedef void (*VisitarEvento)(const Evento *evento, void *contexto);

void pilha_historico_inicializar(PilhaHistorico *pilha);
bool pilha_historico_empilhar(PilhaHistorico *pilha, const Evento *evento);
bool pilha_historico_desempilhar(PilhaHistorico *pilha, Evento *retirado);
bool pilha_historico_topo(const PilhaHistorico *pilha, Evento *do_topo);
void pilha_historico_listar(const PilhaHistorico *pilha,
                            VisitarEvento visitar, void *contexto);
int pilha_historico_tamanho(const PilhaHistorico *pilha);
bool pilha_historico_esta_vazia(const PilhaHistorico *pilha);
void pilha_historico_limpar(PilhaHistorico *pilha);

#endif
