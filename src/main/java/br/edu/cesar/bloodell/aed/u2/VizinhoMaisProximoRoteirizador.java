package br.edu.cesar.bloodell.aed.u2;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;

/**
 * Ordena destinos pela heuristica do vizinho mais proximo em um grafo geografico
 * completo, usando a distancia de Haversine como peso das arestas.
 */
public final class VizinhoMaisProximoRoteirizador implements Roteirizador {

    @Override
    public int[] calcularOrdemDeVisita(double latitudeOrigem, double longitudeOrigem,
                                       double[] latitudesDestinos, double[] longitudesDestinos) {
        validarCoordenadas(latitudeOrigem, longitudeOrigem, "origem");
        if (latitudesDestinos == null || longitudesDestinos == null) {
            throw new IllegalArgumentException("Arrays de coordenadas dos destinos nao podem ser nulos.");
        }
        if (latitudesDestinos.length != longitudesDestinos.length) {
            throw new IllegalArgumentException("Arrays de latitude e longitude devem ter o mesmo tamanho.");
        }

        CoordenadaGeo origem = new CoordenadaGeo(latitudeOrigem, longitudeOrigem);
        CoordenadaGeo[] destinos = new CoordenadaGeo[latitudesDestinos.length];
        for (int indice = 0; indice < destinos.length; indice++) {
            validarCoordenadas(latitudesDestinos[indice], longitudesDestinos[indice], "destino " + indice);
            destinos[indice] = new CoordenadaGeo(latitudesDestinos[indice], longitudesDestinos[indice]);
        }

        int[] ordem = new int[destinos.length];
        boolean[] visitados = new boolean[destinos.length];
        CoordenadaGeo atual = origem;

        for (int posicao = 0; posicao < destinos.length; posicao++) {
            int maisProximo = -1;
            double menorDistancia = Double.POSITIVE_INFINITY;
            for (int indice = 0; indice < destinos.length; indice++) {
                if (visitados[indice]) {
                    continue;
                }
                double distancia = atual.distanciaKm(destinos[indice]);
                // A comparacao estrita preserva o primeiro destino da entrada em empates.
                if (distancia < menorDistancia) {
                    menorDistancia = distancia;
                    maisProximo = indice;
                }
            }
            ordem[posicao] = maisProximo;
            visitados[maisProximo] = true;
            atual = destinos[maisProximo];
        }

        return ordem;
    }

    private static void validarCoordenadas(double latitude, double longitude, String nome) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalida para " + nome + ": " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalida para " + nome + ": " + longitude);
        }
    }
}
