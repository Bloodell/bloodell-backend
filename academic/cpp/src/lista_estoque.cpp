#include "lista_estoque.hpp"

ListaEstoque::ListaEstoque() : cabeca(nullptr), cauda(nullptr), quantidade(0) {}

ListaEstoque::~ListaEstoque() {
    limpar();
}

void ListaEstoque::inserir(const BolsaAed& bolsa) {
    NoBolsa* novo = new NoBolsa(bolsa);
    if (cabeca == nullptr) {
        cabeca = novo;
        cauda = novo;
    } else {
        cauda->proximo = novo;
        cauda = novo;
    }
    quantidade++;
}

bool ListaEstoque::buscarPorCodigo(const std::string& codigo, BolsaAed& encontrada) const {
    NoBolsa* atual = cabeca;
    while (atual != nullptr) {
        if (atual->dado.codigo == codigo) {
            encontrada = atual->dado;
            return true;
        }
        atual = atual->proximo;
    }
    return false;
}

bool ListaEstoque::removerPorCodigo(const std::string& codigo, BolsaAed& removida) {
    NoBolsa* anterior = nullptr;
    NoBolsa* atual = cabeca;
    while (atual != nullptr) {
        if (atual->dado.codigo == codigo) {
            if (anterior == nullptr) {
                cabeca = atual->proximo;
            } else {
                anterior->proximo = atual->proximo;
            }
            if (atual == cauda) {
                cauda = anterior;
            }
            removida = atual->dado;
            delete atual;
            quantidade--;
            return true;
        }
        anterior = atual;
        atual = atual->proximo;
    }
    return false;
}

bool ListaEstoque::primeiraCompativelPorIgualdade(const std::string& tipoSanguineo,
                                                  const std::string& componente,
                                                  const std::string& statusExigido,
                                                  BolsaAed& escolhida) const {
    NoBolsa* atual = cabeca;
    while (atual != nullptr) {
        const bool mesmoTipo = atual->dado.tipoSanguineo == tipoSanguineo;
        const bool mesmoComponente = atual->dado.componente == componente;
        const bool statusOk = statusExigido.empty() || atual->dado.status == statusExigido;
        if (mesmoTipo && mesmoComponente && statusOk) {
            escolhida = atual->dado;
            return true;
        }
        atual = atual->proximo;
    }
    return false;
}

int ListaEstoque::tamanho() const {
    return quantidade;
}

bool ListaEstoque::estaVazia() const {
    return cabeca == nullptr;
}

void ListaEstoque::limpar() {
    NoBolsa* atual = cabeca;
    while (atual != nullptr) {
        NoBolsa* proximo = atual->proximo;
        delete atual;
        atual = proximo;
    }
    cabeca = nullptr;
    cauda = nullptr;
    quantidade = 0;
}
