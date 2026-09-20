package br.edu.cesar.bloodell.aed.u2;

import br.edu.cesar.bloodell.aed.u1.ListaEstoque;

public interface IndiceEstoqueHash {

    void inserir(String chave, ListaEstoque bolsas);

    ListaEstoque buscar(String chave);

    int colisoes();
}
