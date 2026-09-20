#ifndef BLOODELL_PILHA_HISTORICO_HPP
#define BLOODELL_PILHA_HISTORICO_HPP

#include "contratos.hpp"


struct NoEvento {
    EventoAed dado;
    NoEvento* abaixo;

    explicit NoEvento(const EventoAed& valor) : dado(valor), abaixo(nullptr) {}
};

class PilhaHistorico {
public:
    PilhaHistorico();
    ~PilhaHistorico();

    PilhaHistorico(const PilhaHistorico&) = delete;
    PilhaHistorico& operator=(const PilhaHistorico&) = delete;

    void empilhar(const EventoAed& evento);
    bool desempilhar(EventoAed& retirado);
    bool topo(EventoAed& doTopo) const;
    int tamanho() const;
    bool estaVazia() const;
    void limpar();

    const NoEvento* noDoTopo() const { return topoDaPilha; }

private:
    NoEvento* topoDaPilha;
    int quantidade;
};

#endif
