package br.edu.cesar.bloodell.aed.u1;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListaEstoqueTest {

    private static BolsaAed bolsa(String codigo, String tipo, long sequencia) {
        return new BolsaAed(codigo, tipo, "CONCENTRADO_HEMACIAS", "2026-12-01", sequencia, "DISPONIVEL");
    }

    @Test
    @DisplayName("lista mantem a ordem de chegada")
    void listaMantemOrdemDeChegada() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(bolsa("BL-1", "A+", 1));
        lista.inserir(bolsa("BL-2", "O+", 2));
        lista.inserir(bolsa("BL-3", "A+", 3));

        BolsaAed[] itens = lista.paraVetor();
        assertThat(lista.tamanho()).isEqualTo(3);
        assertThat(itens[0].getCodigo()).isEqualTo("BL-1");
        assertThat(itens[1].getCodigo()).isEqualTo("BL-2");
        assertThat(itens[2].getCodigo()).isEqualTo("BL-3");
    }

    @Test
    @DisplayName("remove cabeca, meio e fim religando os nos corretamente")
    void listaRemoveCabecaMeioEFim() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(bolsa("BL-1", "A+", 1));
        lista.inserir(bolsa("BL-2", "A+", 2));
        lista.inserir(bolsa("BL-3", "A+", 3));

        assertThat(lista.removerPorCodigo("BL-2")).isNotNull();
        assertThat(lista.tamanho()).isEqualTo(2);

        assertThat(lista.removerPorCodigo("BL-1")).isNotNull();
        assertThat(lista.paraVetor()[0].getCodigo()).isEqualTo("BL-3");

        assertThat(lista.removerPorCodigo("BL-3")).isNotNull();
        assertThat(lista.estaVazia()).isTrue();
    }

    @Test
    @DisplayName("remover codigo inexistente devolve nulo e nao altera a lista")
    void listaRemoverCodigoInexistenteNaoEhErro() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(bolsa("BL-1", "A+", 1));

        assertThat(lista.removerPorCodigo("BL-999")).isNull();
        assertThat(lista.tamanho()).isEqualTo(1);
    }

    @Test
    @DisplayName("escolhe a primeira da ordem de chegada, e nao a que vence antes (FEFO e U2)")
    void listaEscolhePrimeiraDaOrdemDeChegada() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(new BolsaAed("BL-1", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "DISPONIVEL"));
        lista.inserir(new BolsaAed("BL-9", "A+", "CONCENTRADO_HEMACIAS", "2026-09-25", 9, "DISPONIVEL"));

        BolsaAed escolhida = lista.primeiraCompativelPorIgualdade("A+", "CONCENTRADO_HEMACIAS", "DISPONIVEL");

        assertThat(escolhida).isNotNull();
        assertThat(escolhida.getCodigo())
                .as("se vier BL-9, alguem implementou FEFO na Unidade 1")
                .isEqualTo("BL-1");
    }

    @Test
    @DisplayName("nao escolhe bolsa de tipo diferente: a U1 exige igualdade exata")
    void listaNaoUsaCompatibilidade() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(bolsa("BL-1", "O-", 1));

        assertThat(lista.primeiraCompativelPorIgualdade("A+", "CONCENTRADO_HEMACIAS", "DISPONIVEL"))
                .as("compatibilidade ABO/Rh e conteudo da Unidade 2")
                .isNull();
    }

    @Test
    @DisplayName("ignora bolsa que nao esta disponivel")
    void listaRespeitaStatus() {
        ListaEstoque lista = new ListaEstoque();
        lista.inserir(new BolsaAed("BL-1", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "RESERVADA"));
        lista.inserir(new BolsaAed("BL-2", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 2, "DISPONIVEL"));

        assertThat(lista.primeiraCompativelPorIgualdade("A+", "CONCENTRADO_HEMACIAS", "DISPONIVEL")
                .getCodigo()).isEqualTo("BL-2");
    }

    @Test
    @DisplayName("aguenta mil bolsas preservando a ordem")
    void listaAguentaMilBolsas() {
        ListaEstoque lista = new ListaEstoque();
        for (int indice = 1; indice <= 1000; indice++) {
            lista.inserir(bolsa("BL-" + indice, "O+", indice));
        }

        assertThat(lista.tamanho()).isEqualTo(1000);
        assertThat(lista.buscarPorCodigo("BL-1000").getSequenciaEntrada()).isEqualTo(1000L);
        assertThat(lista.paraVetor()[499].getCodigo()).isEqualTo("BL-500");
    }

    @Test
    @DisplayName("lista vazia responde sem quebrar")
    void listaVazia() {
        ListaEstoque lista = new ListaEstoque();

        assertThat(lista.estaVazia()).isTrue();
        assertThat(lista.tamanho()).isZero();
        assertThat(lista.buscarPorCodigo("BL-1")).isNull();
        assertThat(lista.removerPorCodigo("BL-1")).isNull();
        assertThat(lista.paraVetor()).isEmpty();
    }
}
