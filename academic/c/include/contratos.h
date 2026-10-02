#ifndef BLOODELL_CONTRATOS_H
#define BLOODELL_CONTRATOS_H

#include <stdbool.h>

/*
 * Os contratos usam texto simples para manter o modulo independente do
 * backend. O limite e somente do contrato academico; nenhuma estrutura pronta
 * ou dependencia da aplicacao e necessaria.
 */
#define AED_TEXTO_MAX 128

typedef struct {
    char codigo[AED_TEXTO_MAX];
    char tipo_sanguineo[AED_TEXTO_MAX];
    char componente[AED_TEXTO_MAX];
    char data_validade[AED_TEXTO_MAX];
    long sequencia_entrada;
    char status[AED_TEXTO_MAX];
} Bolsa;

typedef struct {
    char codigo[AED_TEXTO_MAX];
    char hospital[AED_TEXTO_MAX];
    char prioridade[AED_TEXTO_MAX];
    long sequencia_chegada;
} Requisicao;

typedef struct {
    char codigo_bolsa[AED_TEXTO_MAX];
    char status_anterior[AED_TEXTO_MAX];
    char status_novo[AED_TEXTO_MAX];
    char ocorrido_em[AED_TEXTO_MAX];
} Evento;

bool bolsa_inicializar(Bolsa *bolsa, const char *codigo, const char *tipo_sanguineo,
                       const char *componente, const char *data_validade,
                       long sequencia_entrada, const char *status);
bool requisicao_inicializar(Requisicao *requisicao, const char *codigo,
                            const char *hospital, const char *prioridade,
                            long sequencia_chegada);
bool evento_inicializar(Evento *evento, const char *codigo_bolsa,
                        const char *status_anterior, const char *status_novo,
                        const char *ocorrido_em);

#endif
