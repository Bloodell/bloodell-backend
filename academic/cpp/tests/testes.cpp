
#include <cassert>
#include <iostream>
#include <string>

#include "fila_requisicoes.hpp"
#include "lista_estoque.hpp"
#include "pilha_historico.hpp"

static BolsaAed bolsa(const std::string& codigo, const std::string& tipo, long long sequencia) {
    BolsaAed nova;
    nova.codigo = codigo;
    nova.tipoSanguineo = tipo;
    nova.componente = "CONCENTRADO_HEMACIAS";
    nova.dataValidade = "2026-12-01";
    nova.sequenciaEntrada = sequencia;
    nova.status = "DISPONIVEL";
    return nova;
}

static RequisicaoAed requisicao(const std::string& codigo, const std::string& prioridade,
                                long long chegada) {
    RequisicaoAed nova;
    nova.codigo = codigo;
    nova.hospital = "HospitalModelo";
    nova.prioridade = prioridade;
    nova.sequenciaChegada = chegada;
    return nova;
}

static EventoAed evento(const std::string& codigoBolsa, const std::string& statusNovo) {
    EventoAed novo;
    novo.codigoBolsa = codigoBolsa;
    novo.statusAnterior = "-";
    novo.statusNovo = statusNovo;
    novo.ocorridoEm = "2026-09-19T10:00:00Z";
    return novo;
}

static void listaMantemOrdemDeChegada() {
    ListaEstoque lista;
    lista.inserir(bolsa("BL-1", "A+", 1));
    lista.inserir(bolsa("BL-2", "O+", 2));
    lista.inserir(bolsa("BL-3", "A+", 3));

    assert(lista.tamanho() == 3);
    const NoBolsa* no = lista.primeiroNo();
    assert(no->dado.codigo == "BL-1");
    assert(no->proximo->dado.codigo == "BL-2");
    assert(no->proximo->proximo->dado.codigo == "BL-3");
}

static void listaRemoveCabecaMeioEFim() {
    ListaEstoque lista;
    lista.inserir(bolsa("BL-1", "A+", 1));
    lista.inserir(bolsa("BL-2", "A+", 2));
    lista.inserir(bolsa("BL-3", "A+", 3));

    BolsaAed removida;
    assert(lista.removerPorCodigo("BL-2", removida));
    assert(removida.codigo == "BL-2");
    assert(lista.tamanho() == 2);

    assert(lista.removerPorCodigo("BL-1", removida));
    assert(lista.primeiroNo()->dado.codigo == "BL-3");

    assert(lista.removerPorCodigo("BL-3", removida));
    assert(lista.estaVazia());
}

static void listaRemoverCodigoInexistenteNaoEhErro() {
    ListaEstoque lista;
    lista.inserir(bolsa("BL-1", "A+", 1));
    BolsaAed removida;
    assert(!lista.removerPorCodigo("BL-999", removida));
    assert(lista.tamanho() == 1);
}

static void listaEscolhePrimeiraDaOrdemDeChegada() {
    ListaEstoque lista;
    BolsaAed antiga = bolsa("BL-1", "A+", 1);
    antiga.dataValidade = "2026-12-31";
    BolsaAed recente = bolsa("BL-9", "A+", 9);
    recente.dataValidade = "2026-09-25";
    lista.inserir(antiga);
    lista.inserir(recente);

    BolsaAed escolhida;
    assert(lista.primeiraCompativelPorIgualdade("A+", "CONCENTRADO_HEMACIAS", "DISPONIVEL", escolhida));
    assert(escolhida.codigo == "BL-1");
}

static void filaAtendePrioridadeDepoisChegada() {
    FilaRequisicoes fila;
    fila.enfileirar(requisicao("REQ-1", "ELETIVA", 1));
    fila.enfileirar(requisicao("REQ-2", "EMERGENCIA", 2));
    fila.enfileirar(requisicao("REQ-3", "URGENTE", 3));
    fila.enfileirar(requisicao("REQ-4", "EMERGENCIA", 4));

    RequisicaoAed saiu;
    assert(fila.desenfileirar(saiu) && saiu.codigo == "REQ-2");
    assert(fila.desenfileirar(saiu) && saiu.codigo == "REQ-4");
    assert(fila.desenfileirar(saiu) && saiu.codigo == "REQ-3");
    assert(fila.desenfileirar(saiu) && saiu.codigo == "REQ-1");
    assert(!fila.desenfileirar(saiu));
}

static void filaVaziaDevolveFalso() {
    FilaRequisicoes fila;
    RequisicaoAed saiu;
    assert(fila.estaVazia());
    assert(!fila.desenfileirar(saiu));
    assert(!fila.espiar(saiu));
}

static void pilhaEhLifo() {
    PilhaHistorico pilha;
    for (int indice = 1; indice <= 10; indice++) {
        pilha.empilhar(evento("BL-" + std::to_string(indice), "DISPONIVEL"));
    }
    assert(pilha.tamanho() == 10);

    EventoAed retirado;
    for (int indice = 10; indice >= 1; indice--) {
        assert(pilha.desempilhar(retirado));
        assert(retirado.codigoBolsa == "BL-" + std::to_string(indice));
    }
    assert(pilha.estaVazia());
    assert(!pilha.desempilhar(retirado));
}

static void listaAguentaMilBolsas() {
    ListaEstoque lista;
    for (int indice = 1; indice <= 1000; indice++) {
        lista.inserir(bolsa("BL-" + std::to_string(indice), "O+", indice));
    }
    assert(lista.tamanho() == 1000);

    BolsaAed encontrada;
    assert(lista.buscarPorCodigo("BL-1000", encontrada));
    assert(encontrada.sequenciaEntrada == 1000);
}

int main() {
    listaMantemOrdemDeChegada();
    listaRemoveCabecaMeioEFim();
    listaRemoverCodigoInexistenteNaoEhErro();
    listaEscolhePrimeiraDaOrdemDeChegada();
    filaAtendePrioridadeDepoisChegada();
    filaVaziaDevolveFalso();
    pilhaEhLifo();
    listaAguentaMilBolsas();

    std::cout << "Todos os testes C++ das estruturas da U1 passaram." << std::endl;
    return 0;
}
