package br.edu.cesar.bloodell.aed.u2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.aed.u1.ListaEstoque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubsDaUnidadeDoisTest {

    @Test
    @DisplayName("matriz de compatibilidade ainda nao existe")
    void matrizIndisponivel() {
        MatrizCompatibilidade matriz = new AlgoritmosDaUnidadeDois.MatrizIndisponivel();

        assertThatThrownBy(() -> matriz.podeReceber("A+", "O-"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unidade 2");
    }

    @Test
    @DisplayName("FEFO ainda nao existe")
    void fefoIndisponivel() {
        FefoSeletor fefo = new AlgoritmosDaUnidadeDois.FefoIndisponivel();

        assertThatThrownBy(() -> fefo.ordenarPorValidade(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("indice hash ainda nao existe")
    void hashIndisponivel() {
        IndiceEstoqueHash indice = new AlgoritmosDaUnidadeDois.IndiceIndisponivel();

        assertThatThrownBy(() -> indice.buscar("chave"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> indice.inserir("chave", new ListaEstoque()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("roteirizacao ainda nao existe")
    void roteirizadorIndisponivel() {
        Roteirizador roteirizador = new AlgoritmosDaUnidadeDois.RoteirizadorIndisponivel();

        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(
                -8.05, -34.88, new double[]{-8.1}, new double[]{-34.9}))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
