package br.edu.cesar.bloodell.indicadores.aplicacao;

import java.util.Arrays;

public record EstatisticaDescritiva(
        int quantidade,
        double soma,
        double media,
        double mediana,
        double minimo,
        double maximo,
        double desvioPadraoAmostral,
        double coeficienteDeVariacao) {

    public static EstatisticaDescritiva de(double[] valores) {
        if (valores == null || valores.length == 0) {
            return new EstatisticaDescritiva(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double[] ordenados = valores.clone();
        Arrays.sort(ordenados);

        double soma = 0;
        for (double valor : ordenados) {
            soma += valor;
        }
        double media = soma / ordenados.length;

        double mediana;
        int meio = ordenados.length / 2;
        if (ordenados.length % 2 == 0) {
            mediana = (ordenados[meio - 1] + ordenados[meio]) / 2.0;
        } else {
            mediana = ordenados[meio];
        }

        double desvio = 0;
        if (ordenados.length > 1) {
            double somaDosQuadrados = 0;
            for (double valor : ordenados) {
                double diferenca = valor - media;
                somaDosQuadrados += diferenca * diferenca;
            }
            desvio = Math.sqrt(somaDosQuadrados / (ordenados.length - 1));
        }

        double cv = media == 0 ? 0 : (desvio / media) * 100.0;

        return new EstatisticaDescritiva(ordenados.length, arredondar(soma), arredondar(media),
                arredondar(mediana), arredondar(ordenados[0]), arredondar(ordenados[ordenados.length - 1]),
                arredondar(desvio), arredondar(cv));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
