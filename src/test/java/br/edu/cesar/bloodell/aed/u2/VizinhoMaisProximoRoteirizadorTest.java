package br.edu.cesar.bloodell.aed.u2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VizinhoMaisProximoRoteirizadorTest {

    private final Roteirizador roteirizador = new VizinhoMaisProximoRoteirizador();

    @Test
    @DisplayName("sem destinos devolve ordem vazia")
    void semDestinos() {
        assertThat(roteirizador.calcularOrdemDeVisita(0, 0, new double[0], new double[0]))
                .isEmpty();
    }

    @Test
    @DisplayName("um destino e visitado sem retornar a origem")
    void umDestino() {
        assertThat(roteirizador.calcularOrdemDeVisita(0, 0, new double[]{0}, new double[]{1}))
                .containsExactly(0);
    }

    @Test
    @DisplayName("escolhe o vizinho mais proximo a partir da posicao atual")
    void multiplosDestinos() {
        int[] ordem = roteirizador.calcularOrdemDeVisita(
                0, 0, new double[]{0, 0, 0}, new double[]{2, 1, 3});

        assertThat(ordem).containsExactly(1, 0, 2);
    }

    @Test
    @DisplayName("empate preserva a ordem original dos destinos")
    void desempatePelaOrdemDeEntrada() {
        int[] ordem = roteirizador.calcularOrdemDeVisita(
                0, 0, new double[]{1, -1}, new double[]{0, 0});

        assertThat(ordem).containsExactly(0, 1);
    }

    @Test
    @DisplayName("destinos com coordenadas repetidas sao todos visitados em ordem")
    void coordenadasRepetidas() {
        int[] ordem = roteirizador.calcularOrdemDeVisita(
                0, 0, new double[]{1, 1, 1}, new double[]{1, 1, 1});

        assertThat(ordem).containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("nao altera os arrays recebidos")
    void preservaArraysDeEntrada() {
        double[] latitudes = {0, 0};
        double[] longitudes = {2, 1};

        roteirizador.calcularOrdemDeVisita(0, 0, latitudes, longitudes);

        assertThat(latitudes).containsExactly(0, 0);
        assertThat(longitudes).containsExactly(2, 1);
    }

    @Test
    @DisplayName("rejeita arrays nulos ou de tamanhos diferentes")
    void arraysInvalidos() {
        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(0, 0, null, new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(0, 0, new double[1], new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejeita coordenadas fora do intervalo e valores nao finitos")
    void coordenadasInvalidas() {
        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(91, 0, new double[0], new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(0, 0, new double[]{Double.NaN}, new double[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> roteirizador.calcularOrdemDeVisita(0, 0, new double[]{0}, new double[]{181}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
