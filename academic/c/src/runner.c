#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "fila_requisicoes.h"
#include "lista_estoque.h"
#include "pilha_historico.h"

static void imprimir_bolsa(const Bolsa *bolsa, void *contexto) {
    int *posicao = contexto;
    printf("%d %s\n", *posicao, bolsa->codigo);
    (*posicao)++;
}

static void imprimir_requisicao(const Requisicao *requisicao, void *contexto) {
    int *posicao = contexto;
    printf("%d %s\n", *posicao, requisicao->codigo);
    (*posicao)++;
}

static void imprimir_evento(const Evento *evento, void *contexto) {
    int *posicao = contexto;
    printf("%d %s\n", *posicao, evento->codigo_bolsa);
    (*posicao)++;
}

int main(void) {
    ListaEstoque lista;
    FilaRequisicoes fila;
    PilhaHistorico pilha;
    char linha[512];

    lista_estoque_inicializar(&lista);
    fila_requisicoes_inicializar(&fila);
    pilha_historico_inicializar(&pilha);

    while (fgets(linha, sizeof(linha), stdin) != NULL) {
        char comando[AED_TEXTO_MAX];

        if (sscanf(linha, "%127s", comando) != 1) {
            continue;
        }
        if (strcmp(comando, "ESTRUTURA") == 0) {
            char qual[AED_TEXTO_MAX];
            sscanf(linha, "%*s %127s", qual);
            printf("ESTRUTURA %s\n", qual);
        } else if (strcmp(comando, "INSERIR") == 0) {
            Bolsa bolsa;
            char codigo[AED_TEXTO_MAX], tipo[AED_TEXTO_MAX], componente[AED_TEXTO_MAX];
            char validade[AED_TEXTO_MAX], status[AED_TEXTO_MAX];
            long sequencia;
            int lidos = sscanf(linha, "%*s %127s %127s %127s %127s %ld %127s",
                               codigo, tipo, componente, validade, &sequencia, status);
            if (lidos == 6 && bolsa_inicializar(&bolsa, codigo, tipo, componente,
                                                validade, sequencia, status)
                    && lista_estoque_inserir(&lista, &bolsa)) {
                printf("OK %d\n", lista_estoque_tamanho(&lista));
            }
        } else if (strcmp(comando, "REMOVER") == 0) {
            char codigo[AED_TEXTO_MAX];
            Bolsa removida;
            sscanf(linha, "%*s %127s", codigo);
            if (lista_estoque_remover_por_codigo(&lista, codigo, &removida)) {
                printf("REMOVIDO %s\n", removida.codigo);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "BUSCAR") == 0) {
            char codigo[AED_TEXTO_MAX];
            Bolsa encontrada;
            sscanf(linha, "%*s %127s", codigo);
            if (lista_estoque_buscar_por_codigo(&lista, codigo, &encontrada)) {
                printf("ENCONTRADO %s\n", encontrada.codigo);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "PRIMEIRA") == 0) {
            char tipo[AED_TEXTO_MAX], componente[AED_TEXTO_MAX], status[AED_TEXTO_MAX];
            Bolsa escolhida;
            sscanf(linha, "%*s %127s %127s %127s", tipo, componente, status);
            if (lista_estoque_primeira_compativel_por_igualdade(
                        &lista, tipo, componente, status, &escolhida)) {
                printf("ESCOLHIDA %s %ld\n", escolhida.codigo,
                       escolhida.sequencia_entrada);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "ENFILEIRAR") == 0) {
            Requisicao requisicao;
            char codigo[AED_TEXTO_MAX], hospital[AED_TEXTO_MAX], prioridade[AED_TEXTO_MAX];
            long sequencia;
            int lidos = sscanf(linha, "%*s %127s %127s %127s %ld",
                               codigo, hospital, prioridade, &sequencia);
            if (lidos == 4 && requisicao_inicializar(&requisicao, codigo, hospital,
                                                     prioridade, sequencia)
                    && fila_requisicoes_enfileirar(&fila, &requisicao)) {
                printf("OK %d\n", fila_requisicoes_tamanho(&fila));
            }
        } else if (strcmp(comando, "DESENFILEIRAR") == 0) {
            Requisicao retirada;
            if (fila_requisicoes_desenfileirar(&fila, &retirada)) {
                printf("SAIU %s %s\n", retirada.codigo, retirada.prioridade);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "ESPIAR") == 0) {
            Requisicao proxima;
            if (fila_requisicoes_espiar(&fila, &proxima)) {
                printf("PROXIMA %s\n", proxima.codigo);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "EMPILHAR") == 0) {
            Evento evento;
            char codigo[AED_TEXTO_MAX], anterior[AED_TEXTO_MAX], novo[AED_TEXTO_MAX];
            char ocorrido[AED_TEXTO_MAX];
            int lidos = sscanf(linha, "%*s %127s %127s %127s %127s",
                               codigo, anterior, novo, ocorrido);
            if (lidos == 4 && evento_inicializar(&evento, codigo, anterior, novo, ocorrido)
                    && pilha_historico_empilhar(&pilha, &evento)) {
                printf("OK %d\n", pilha_historico_tamanho(&pilha));
            }
        } else if (strcmp(comando, "DESEMPILHAR") == 0) {
            Evento retirado;
            if (pilha_historico_desempilhar(&pilha, &retirado)) {
                printf("SAIU %s %s\n", retirado.codigo_bolsa, retirado.status_novo);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "TOPO") == 0) {
            Evento do_topo;
            if (pilha_historico_topo(&pilha, &do_topo)) {
                printf("TOPO %s %s\n", do_topo.codigo_bolsa, do_topo.status_novo);
            } else {
                printf("NULO\n");
            }
        } else if (strcmp(comando, "LISTAR") == 0) {
            char qual[AED_TEXTO_MAX];
            int posicao = 1;
            sscanf(linha, "%*s %127s", qual);
            if (strcmp(qual, "LISTA") == 0) {
                printf("LISTAGEM %d\n", lista_estoque_tamanho(&lista));
                lista_estoque_listar(&lista, imprimir_bolsa, &posicao);
            } else if (strcmp(qual, "FILA") == 0) {
                printf("LISTAGEM %d\n", fila_requisicoes_tamanho(&fila));
                fila_requisicoes_listar(&fila, imprimir_requisicao, &posicao);
            } else {
                printf("LISTAGEM %d\n", pilha_historico_tamanho(&pilha));
                pilha_historico_listar(&pilha, imprimir_evento, &posicao);
            }
        } else if (strcmp(comando, "TAMANHO") == 0) {
            char qual[AED_TEXTO_MAX];
            sscanf(linha, "%*s %127s", qual);
            if (strcmp(qual, "LISTA") == 0) {
                printf("TAMANHO %d\n", lista_estoque_tamanho(&lista));
            } else if (strcmp(qual, "FILA") == 0) {
                printf("TAMANHO %d\n", fila_requisicoes_tamanho(&fila));
            } else {
                printf("TAMANHO %d\n", pilha_historico_tamanho(&pilha));
            }
        } else {
            printf("COMANDO_DESCONHECIDO %s\n", comando);
        }
    }

    lista_estoque_limpar(&lista);
    fila_requisicoes_limpar(&fila);
    pilha_historico_limpar(&pilha);
    return EXIT_SUCCESS;
}
