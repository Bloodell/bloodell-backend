package br.edu.cesar.bloodell.aed.u2;

public interface Roteirizador {

    /**
     * Devolve os indices, base zero, dos destinos na ordem do vizinho mais proximo.
     * Empates preservam a ordem de entrada e a rota nao retorna a origem.
     */
    int[] calcularOrdemDeVisita(double latitudeOrigem, double longitudeOrigem,
                                double[] latitudesDestinos, double[] longitudesDestinos);
}
