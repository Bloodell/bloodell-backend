#include "fila_requisicoes.hpp"

#include <stdexcept>

static const char* PRIORIDADES[QUANTIDADE_DE_PRIORIDADES] = {"EMERGENCIA", "URGENTE", "ELETIVA"};

FilaRequisicoes::FilaRequisicoes() : quantidade(0) {
    for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        cabecas[faixa] = nullptr;
        caudas[faixa] = nullptr;
    }
}

FilaRequisicoes::~FilaRequisicoes() {
    limpar();
}

int FilaRequisicoes::indiceDaPrioridade(const std::string& prioridade) {
    for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        if (prioridade == PRIORIDADES[faixa]) {
            return faixa;
        }
    }
    throw std::invalid_argument("Prioridade desconhecida na fila: " + prioridade);
}

void FilaRequisicoes::enfileirar(const RequisicaoAed& requisicao) {
    const int faixa = indiceDaPrioridade(requisicao.prioridade);
    NoRequisicao* novo = new NoRequisicao(requisicao);
    if (cabecas[faixa] == nullptr) {
        cabecas[faixa] = novo;
        caudas[faixa] = novo;
    } else {
        caudas[faixa]->proximo = novo;
        caudas[faixa] = novo;
    }
    quantidade++;
}

bool FilaRequisicoes::desenfileirar(RequisicaoAed& retirada) {
    for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        NoRequisicao* cabeca = cabecas[faixa];
        if (cabeca != nullptr) {
            cabecas[faixa] = cabeca->proximo;
            if (cabecas[faixa] == nullptr) {
                caudas[faixa] = nullptr;
            }
            retirada = cabeca->dado;
            delete cabeca;
            quantidade--;
            return true;
        }
    }
    return false;
}

bool FilaRequisicoes::espiar(RequisicaoAed& proxima) const {
    for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        if (cabecas[faixa] != nullptr) {
            proxima = cabecas[faixa]->dado;
            return true;
        }
    }
    return false;
}

int FilaRequisicoes::tamanho() const {
    return quantidade;
}

bool FilaRequisicoes::estaVazia() const {
    return quantidade == 0;
}

void FilaRequisicoes::limpar() {
    for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
        NoRequisicao* atual = cabecas[faixa];
        while (atual != nullptr) {
            NoRequisicao* proximo = atual->proximo;
            delete atual;
            atual = proximo;
        }
        cabecas[faixa] = nullptr;
        caudas[faixa] = nullptr;
    }
    quantidade = 0;
}
