package br.edu.cesar.bloodell.compartilhado.dominio;

import java.util.Objects;

public class FaixaTemperatura {

    private final double minimaCelsius;
    private final double maximaCelsius;

    public FaixaTemperatura(double minimaCelsius, double maximaCelsius) {
        if (minimaCelsius >= maximaCelsius) {
            throw new IllegalArgumentException("Temperatura minima deve ser menor que a maxima.");
        }
        this.minimaCelsius = minimaCelsius;
        this.maximaCelsius = maximaCelsius;
    }

    public double getMinimaCelsius() {
        return minimaCelsius;
    }

    public double getMaximaCelsius() {
        return maximaCelsius;
    }

    public boolean contem(double temperaturaCelsius) {
        return temperaturaCelsius >= minimaCelsius && temperaturaCelsius <= maximaCelsius;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof FaixaTemperatura)) {
            return false;
        }
        FaixaTemperatura faixa = (FaixaTemperatura) outro;
        return Double.compare(minimaCelsius, faixa.minimaCelsius) == 0
                && Double.compare(maximaCelsius, faixa.maximaCelsius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimaCelsius, maximaCelsius);
    }

    @Override
    public String toString() {
        return minimaCelsius + " a " + maximaCelsius + " C";
    }
}
