package br.edu.cesar.bloodell.alocacao.dominio;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;

public interface PoliticaDeAlocacao {

    String nome();

    String aviso();

    BolsaAed escolher(ListaEstoque estoque, String tipoSolicitado, String componente, DiarioDaDecisao diario);
}
