#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "fila_requisicoes.h"
#include "lista_estoque.h"
#include "pilha_historico.h"

static Bolsa bolsa(const char *codigo, const char *tipo, long sequencia,
                   const char *validade, const char *status) {
    Bolsa resultado;
    assert(bolsa_inicializar(&resultado, codigo, tipo, "CONCENTRADO_HEMACIAS",
                             validade, sequencia, status));
    return resultado;
}

static Requisicao requisicao(const char *codigo, const char *prioridade, long chegada) {
    Requisicao resultado;
    assert(requisicao_inicializar(&resultado, codigo, "HospitalModelo", prioridade, chegada));
    return resultado;
}

static Evento evento(const char *codigo, const char *status) {
    Evento resultado;
    assert(evento_inicializar(&resultado, codigo, "ANTERIOR", status,
                              "2026-09-19T10:00:00Z"));
    return resultado;
}

static void testar_lista(void) {
    ListaEstoque lista;
    Bolsa bolsa_1 = bolsa("BL-1", "A+", 1, "2026-12-31", "DISPONIVEL");
    Bolsa bolsa_2 = bolsa("BL-2", "A+", 2, "2026-12-30", "DISPONIVEL");
    Bolsa bolsa_3 = bolsa("BL-3", "A+", 3, "2026-12-29", "DISPONIVEL");
    Bolsa bolsa_9 = bolsa("BL-9", "A+", 9, "2026-09-25", "DISPONIVEL");
    Bolsa removida;
    Bolsa encontrada;
    Bolsa escolhida;

    lista_estoque_inicializar(&lista);
    assert(lista_estoque_esta_vazia(&lista));
    assert(lista_estoque_buscar_por_codigo(&lista, "BL-1", &encontrada) == false);

    assert(lista_estoque_inserir(&lista, &bolsa_1));
    assert(lista_estoque_inserir(&lista, &bolsa_2));
    assert(lista_estoque_inserir(&lista, &bolsa_3));
    assert(lista_estoque_tamanho(&lista) == 3);

    assert(lista_estoque_buscar_por_codigo(&lista, "BL-2", &encontrada));
    assert(encontrada.sequencia_entrada == 2);
    assert(lista_estoque_remover_por_codigo(&lista, "BL-2", &removida));
    assert(strcmp(removida.codigo, "BL-2") == 0);
    assert(lista_estoque_remover_por_codigo(&lista, "BL-1", &removida));
    assert(lista_estoque_remover_por_codigo(&lista, "BL-3", &removida));
    assert(lista_estoque_esta_vazia(&lista));
    assert(lista_estoque_remover_por_codigo(&lista, "BL-999", &removida) == false);

    assert(lista_estoque_inserir(&lista, &bolsa_1));
    assert(lista_estoque_inserir(&lista, &bolsa_9));
    assert(lista_estoque_primeira_compativel_por_igualdade(
            &lista, "A+", "CONCENTRADO_HEMACIAS", "DISPONIVEL", &escolhida));
    assert(strcmp(escolhida.codigo, "BL-1") == 0);
    assert(!lista_estoque_primeira_compativel_por_igualdade(
            &lista, "O-", "CONCENTRADO_HEMACIAS", "DISPONIVEL", &escolhida));
    lista_estoque_limpar(&lista);
    assert(lista_estoque_esta_vazia(&lista));
    assert(lista_estoque_tamanho(&lista) == 0);
}

static void testar_fila(void) {
    FilaRequisicoes fila;
    Requisicao req_1 = requisicao("REQ-1", "ELETIVA", 1);
    Requisicao req_2 = requisicao("REQ-2", "EMERGENCIA", 2);
    Requisicao req_3 = requisicao("REQ-3", "URGENTE", 3);
    Requisicao req_4 = requisicao("REQ-4", "EMERGENCIA", 4);
    Requisicao req_invalida = requisicao("REQ-X", "INVALIDA", 5);
    Requisicao retirada;
    Requisicao proxima;

    fila_requisicoes_inicializar(&fila);
    assert(fila_requisicoes_esta_vazia(&fila));
    assert(!fila_requisicoes_desenfileirar(&fila, &retirada));
    assert(!fila_requisicoes_espiar(&fila, &proxima));

    assert(fila_requisicoes_enfileirar(&fila, &req_1));
    assert(fila_requisicoes_enfileirar(&fila, &req_2));
    assert(fila_requisicoes_enfileirar(&fila, &req_3));
    assert(fila_requisicoes_enfileirar(&fila, &req_4));
    assert(fila_requisicoes_espiar(&fila, &proxima));
    assert(strcmp(proxima.codigo, "REQ-2") == 0);
    assert(fila_requisicoes_tamanho(&fila) == 4);

    assert(fila_requisicoes_desenfileirar(&fila, &retirada));
    assert(strcmp(retirada.codigo, "REQ-2") == 0);
    assert(fila_requisicoes_desenfileirar(&fila, &retirada));
    assert(strcmp(retirada.codigo, "REQ-4") == 0);
    assert(fila_requisicoes_desenfileirar(&fila, &retirada));
    assert(strcmp(retirada.codigo, "REQ-3") == 0);
    assert(fila_requisicoes_desenfileirar(&fila, &retirada));
    assert(strcmp(retirada.codigo, "REQ-1") == 0);
    assert(fila_requisicoes_esta_vazia(&fila));
    assert(!fila_requisicoes_enfileirar(&fila, &req_invalida));
    fila_requisicoes_limpar(&fila);
    assert(fila_requisicoes_tamanho(&fila) == 0);
}

static void testar_pilha(void) {
    PilhaHistorico pilha;
    Evento evento_1 = evento("BL-1", "RECEBIDA");
    Evento evento_2 = evento("BL-2", "ARMAZENADA");
    Evento evento_3 = evento("BL-3", "DISPONIVEL");
    Evento retirado;
    Evento do_topo;

    pilha_historico_inicializar(&pilha);
    assert(pilha_historico_esta_vazia(&pilha));
    assert(!pilha_historico_desempilhar(&pilha, &retirado));
    assert(!pilha_historico_topo(&pilha, &do_topo));

    assert(pilha_historico_empilhar(&pilha, &evento_1));
    assert(pilha_historico_empilhar(&pilha, &evento_2));
    assert(pilha_historico_empilhar(&pilha, &evento_3));
    assert(pilha_historico_topo(&pilha, &do_topo));
    assert(strcmp(do_topo.codigo_bolsa, "BL-3") == 0);
    assert(pilha_historico_desempilhar(&pilha, &retirado));
    assert(strcmp(retirado.codigo_bolsa, "BL-3") == 0);
    assert(pilha_historico_desempilhar(&pilha, &retirado));
    assert(strcmp(retirado.codigo_bolsa, "BL-2") == 0);
    assert(pilha_historico_desempilhar(&pilha, &retirado));
    assert(strcmp(retirado.codigo_bolsa, "BL-1") == 0);
    assert(pilha_historico_esta_vazia(&pilha));
    pilha_historico_limpar(&pilha);
    assert(pilha_historico_tamanho(&pilha) == 0);
}

int main(void) {
    testar_lista();
    testar_fila();
    testar_pilha();
    puts("Testes C da AED U1: OK");
    return 0;
}
