#include "pilha_historico.hpp"

PilhaHistorico::PilhaHistorico() : topoDaPilha(nullptr), quantidade(0) {}

PilhaHistorico::~PilhaHistorico() {
    limpar();
}

void PilhaHistorico::empilhar(const EventoAed& evento) {
    NoEvento* novo = new NoEvento(evento);
    novo->abaixo = topoDaPilha;
    topoDaPilha = novo;
    quantidade++;
}

bool PilhaHistorico::desempilhar(EventoAed& retirado) {
    if (topoDaPilha == nullptr) {
        return false;
    }
    NoEvento* removido = topoDaPilha;
    topoDaPilha = removido->abaixo;
    retirado = removido->dado;
    delete removido;
    quantidade--;
    return true;
}

bool PilhaHistorico::topo(EventoAed& doTopo) const {
    if (topoDaPilha == nullptr) {
        return false;
    }
    doTopo = topoDaPilha->dado;
    return true;
}

int PilhaHistorico::tamanho() const {
    return quantidade;
}

bool PilhaHistorico::estaVazia() const {
    return topoDaPilha == nullptr;
}

void PilhaHistorico::limpar() {
    NoEvento* atual = topoDaPilha;
    while (atual != nullptr) {
        NoEvento* abaixo = atual->abaixo;
        delete atual;
        atual = abaixo;
    }
    topoDaPilha = nullptr;
    quantidade = 0;
}
