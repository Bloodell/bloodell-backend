package br.edu.cesar.bloodell.alocacao.dominio;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;

public class AlocacaoProvisoriaU1 implements PoliticaDeAlocacao {

    public static final String AVISO =
            "Alocacao PROVISORIA da Unidade 1: primeira bolsa disponivel do MESMO tipo, "
            + "pela ordem de chegada. Sem matriz ABO/Rh, sem FEFO, sem hash e sem rota "
            + "-- esses algoritmos sao da Unidade 2.";

    @Override
    public String nome() {
        return "PROVISORIA_U1";
    }

    @Override
    public String aviso() {
        return AVISO;
    }

    @Override
    public BolsaAed escolher(ListaEstoque estoque, String tipoSolicitado, String componente,
                             DiarioDaDecisao diario) {
        diario.anotar("estoque",
                "Lista da AED-U1 hidratada do banco na ordem de chegada",
                estoque.tamanho() + " bolsa(s) na lista");

        diario.anotar("filtro",
                "Procurando a primeira bolsa DISPONIVEL com tipo exatamente igual a "
                        + tipoSolicitado + " e componente " + componente,
                "percorrendo no a no");

        BolsaAed escolhida = estoque.primeiraCompativelPorIgualdade(tipoSolicitado, componente, "DISPONIVEL");

        if (escolhida == null) {
            diario.anotar("resultado",
                    "Nenhuma bolsa disponivel atende ao pedido com igualdade de tipo",
                    "sem bolsa elegivel");
        } else {
            diario.anotar("escolha",
                    "Primeira da lista que atendeu: " + escolhida.getCodigo()
                            + " (chegada #" + escolhida.getSequenciaEntrada()
                            + ", validade " + escolhida.getDataValidade() + ")",
                    "bolsa selecionada");
            diario.anotar("limite-academico",
                    "A validade foi apenas VERIFICADA, nao usada para ordenar. Ordenar por "
                            + "validade e FEFO, conteudo da Unidade 2.",
                    "regra da U1 respeitada");
        }
        return escolhida;
    }
}
