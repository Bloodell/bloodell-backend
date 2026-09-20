#ifndef BLOODELL_FILA_REQUISICOES_HPP
#define BLOODELL_FILA_REQUISICOES_HPP

#include "contratos.hpp"


struct NoRequisicao {
    RequisicaoAed dado;
    NoRequisicao* proximo;

    explicit NoRequisicao(const RequisicaoAed& valor) : dado(valor), proximo(nullptr) {}
};

const int QUANTIDADE_DE_PRIORIDADES = 3;

class FilaRequisicoes {
public:
    FilaRequisicoes();
    ~FilaRequisicoes();

    FilaRequisicoes(const FilaRequisicoes&) = delete;
    FilaRequisicoes& operator=(const FilaRequisicoes&) = delete;

    void enfileirar(const RequisicaoAed& requisicao);
    bool desenfileirar(RequisicaoAed& retirada);
    bool espiar(RequisicaoAed& proxima) const;
    int tamanho() const;
    bool estaVazia() const;
    void limpar();

    const NoRequisicao* cabecaDaFaixa(int faixa) const { return cabecas[faixa]; }

private:
    NoRequisicao* cabecas[QUANTIDADE_DE_PRIORIDADES];
    NoRequisicao* caudas[QUANTIDADE_DE_PRIORIDADES];
    int quantidade;

    static int indiceDaPrioridade(const std::string& prioridade);
};

#endif
