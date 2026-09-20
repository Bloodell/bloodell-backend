
#include <iostream>
#include <sstream>
#include <string>

#include "fila_requisicoes.hpp"
#include "lista_estoque.hpp"
#include "pilha_historico.hpp"

int main() {
    ListaEstoque lista;
    FilaRequisicoes fila;
    PilhaHistorico pilha;

    std::string linha;
    while (std::getline(std::cin, linha)) {
        if (linha.empty()) {
            continue;
        }
        std::istringstream entrada(linha);
        std::string comando;
        entrada >> comando;

        if (comando == "ESTRUTURA") {
            std::string qual;
            entrada >> qual;
            std::cout << "ESTRUTURA " << qual << "\n";

        } else if (comando == "INSERIR") {
            BolsaAed bolsa;
            entrada >> bolsa.codigo >> bolsa.tipoSanguineo >> bolsa.componente
                    >> bolsa.dataValidade >> bolsa.sequenciaEntrada >> bolsa.status;
            lista.inserir(bolsa);
            std::cout << "OK " << lista.tamanho() << "\n";

        } else if (comando == "REMOVER") {
            std::string codigo;
            entrada >> codigo;
            BolsaAed removida;
            if (lista.removerPorCodigo(codigo, removida)) {
                std::cout << "REMOVIDO " << removida.codigo << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "BUSCAR") {
            std::string codigo;
            entrada >> codigo;
            BolsaAed encontrada;
            if (lista.buscarPorCodigo(codigo, encontrada)) {
                std::cout << "ENCONTRADO " << encontrada.codigo << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "PRIMEIRA") {
            std::string tipo;
            std::string componente;
            std::string status;
            entrada >> tipo >> componente >> status;
            BolsaAed escolhida;
            if (lista.primeiraCompativelPorIgualdade(tipo, componente, status, escolhida)) {
                std::cout << "ESCOLHIDA " << escolhida.codigo << " "
                          << escolhida.sequenciaEntrada << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "ENFILEIRAR") {
            RequisicaoAed requisicao;
            entrada >> requisicao.codigo >> requisicao.hospital
                    >> requisicao.prioridade >> requisicao.sequenciaChegada;
            fila.enfileirar(requisicao);
            std::cout << "OK " << fila.tamanho() << "\n";

        } else if (comando == "DESENFILEIRAR") {
            RequisicaoAed retirada;
            if (fila.desenfileirar(retirada)) {
                std::cout << "SAIU " << retirada.codigo << " " << retirada.prioridade << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "ESPIAR") {
            RequisicaoAed proxima;
            if (fila.espiar(proxima)) {
                std::cout << "PROXIMA " << proxima.codigo << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "EMPILHAR") {
            EventoAed evento;
            entrada >> evento.codigoBolsa >> evento.statusAnterior
                    >> evento.statusNovo >> evento.ocorridoEm;
            pilha.empilhar(evento);
            std::cout << "OK " << pilha.tamanho() << "\n";

        } else if (comando == "DESEMPILHAR") {
            EventoAed retirado;
            if (pilha.desempilhar(retirado)) {
                std::cout << "SAIU " << retirado.codigoBolsa << " " << retirado.statusNovo << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "TOPO") {
            EventoAed doTopo;
            if (pilha.topo(doTopo)) {
                std::cout << "TOPO " << doTopo.codigoBolsa << " " << doTopo.statusNovo << "\n";
            } else {
                std::cout << "NULO\n";
            }

        } else if (comando == "LISTAR") {
            std::string qual;
            entrada >> qual;
            if (qual == "LISTA") {
                std::cout << "LISTAGEM " << lista.tamanho() << "\n";
                int posicao = 1;
                for (const NoBolsa* no = lista.primeiroNo(); no != nullptr; no = no->proximo) {
                    std::cout << posicao << " " << no->dado.codigo << "\n";
                    posicao++;
                }
            } else if (qual == "FILA") {
                std::cout << "LISTAGEM " << fila.tamanho() << "\n";
                int posicao = 1;
                for (int faixa = 0; faixa < QUANTIDADE_DE_PRIORIDADES; faixa++) {
                    for (const NoRequisicao* no = fila.cabecaDaFaixa(faixa);
                         no != nullptr; no = no->proximo) {
                        std::cout << posicao << " " << no->dado.codigo << "\n";
                        posicao++;
                    }
                }
            } else {
                std::cout << "LISTAGEM " << pilha.tamanho() << "\n";
                int posicao = 1;
                for (const NoEvento* no = pilha.noDoTopo(); no != nullptr; no = no->abaixo) {
                    std::cout << posicao << " " << no->dado.codigoBolsa << "\n";
                    posicao++;
                }
            }

        } else if (comando == "TAMANHO") {
            std::string qual;
            entrada >> qual;
            if (qual == "LISTA") {
                std::cout << "TAMANHO " << lista.tamanho() << "\n";
            } else if (qual == "FILA") {
                std::cout << "TAMANHO " << fila.tamanho() << "\n";
            } else {
                std::cout << "TAMANHO " << pilha.tamanho() << "\n";
            }

        } else {
            std::cout << "COMANDO_DESCONHECIDO " << comando << "\n";
        }
    }
    return 0;
}
