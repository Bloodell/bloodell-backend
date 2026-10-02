#include "contratos.h"

#include <string.h>

static bool copiar_texto(char destino[AED_TEXTO_MAX], const char *origem) {
    size_t tamanho;

    if (destino == NULL || origem == NULL) {
        return false;
    }

    tamanho = strlen(origem);
    if (tamanho >= AED_TEXTO_MAX) {
        return false;
    }

    memcpy(destino, origem, tamanho + 1);
    return true;
}

bool bolsa_inicializar(Bolsa *bolsa, const char *codigo, const char *tipo_sanguineo,
                       const char *componente, const char *data_validade,
                       long sequencia_entrada, const char *status) {
    if (bolsa == NULL || !copiar_texto(bolsa->codigo, codigo)
            || !copiar_texto(bolsa->tipo_sanguineo, tipo_sanguineo)
            || !copiar_texto(bolsa->componente, componente)
            || !copiar_texto(bolsa->data_validade, data_validade)
            || !copiar_texto(bolsa->status, status)) {
        return false;
    }
    bolsa->sequencia_entrada = sequencia_entrada;
    return true;
}

bool requisicao_inicializar(Requisicao *requisicao, const char *codigo,
                            const char *hospital, const char *prioridade,
                            long sequencia_chegada) {
    if (requisicao == NULL || !copiar_texto(requisicao->codigo, codigo)
            || !copiar_texto(requisicao->hospital, hospital)
            || !copiar_texto(requisicao->prioridade, prioridade)) {
        return false;
    }
    requisicao->sequencia_chegada = sequencia_chegada;
    return true;
}

bool evento_inicializar(Evento *evento, const char *codigo_bolsa,
                        const char *status_anterior, const char *status_novo,
                        const char *ocorrido_em) {
    if (evento == NULL || !copiar_texto(evento->codigo_bolsa, codigo_bolsa)
            || !copiar_texto(evento->status_anterior, status_anterior)
            || !copiar_texto(evento->status_novo, status_novo)
            || !copiar_texto(evento->ocorrido_em, ocorrido_em)) {
        return false;
    }
    return true;
}
