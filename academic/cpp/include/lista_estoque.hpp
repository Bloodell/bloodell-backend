#ifndef BLOODELL_LISTA_ESTOQUE_HPP
#define BLOODELL_LISTA_ESTOQUE_HPP

#include "contratos.hpp"


struct NoBolsa {
    BolsaAed dado;
    NoBolsa* proximo;

    explicit NoBolsa(const BolsaAed& valor) : dado(valor), proximo(nullptr) {}
};

class ListaEstoque {
public:
    ListaEstoque();
    ~ListaEstoque();

    ListaEstoque(const ListaEstoque&) = delete;
    ListaEstoque& operator=(const ListaEstoque&) = delete;

    void inserir(const BolsaAed& bolsa);
    bool buscarPorCodigo(const std::string& codigo, BolsaAed& encontrada) const;
    bool removerPorCodigo(const std::string& codigo, BolsaAed& removida);
    bool primeiraCompativelPorIgualdade(const std::string& tipoSanguineo,
                                        const std::string& componente,
                                        const std::string& statusExigido,
                                        BolsaAed& escolhida) const;
    int tamanho() const;
    bool estaVazia() const;
    void limpar();

    const NoBolsa* primeiroNo() const { return cabeca; }

private:
    NoBolsa* cabeca;
    NoBolsa* cauda;
    int quantidade;
};

#endif
