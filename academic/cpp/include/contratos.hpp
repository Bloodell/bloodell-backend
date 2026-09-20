#ifndef BLOODELL_CONTRATOS_HPP
#define BLOODELL_CONTRATOS_HPP

#include <string>


struct BolsaAed {
    std::string codigo;
    std::string tipoSanguineo;
    std::string componente;
    std::string dataValidade;
    long long sequenciaEntrada;
    std::string status;
};

struct RequisicaoAed {
    std::string codigo;
    std::string hospital;
    std::string prioridade;
    long long sequenciaChegada;
};

struct EventoAed {
    std::string codigoBolsa;
    std::string statusAnterior;
    std::string statusNovo;
    std::string ocorridoEm;
};

#endif
