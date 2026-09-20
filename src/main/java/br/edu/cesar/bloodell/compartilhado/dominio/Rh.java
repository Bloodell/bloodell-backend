package br.edu.cesar.bloodell.compartilhado.dominio;

public enum Rh {
    POSITIVO("+"),
    NEGATIVO("-");

    private final String sinal;

    Rh(String sinal) {
        this.sinal = sinal;
    }

    public String getSinal() {
        return sinal;
    }

    public static Rh doTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Fator Rh nao informado.");
        }
        String limpo = texto.trim();
        for (Rh valor : values()) {
            if (valor.sinal.equals(limpo) || valor.name().equalsIgnoreCase(limpo)) {
                return valor;
            }
        }
        throw new IllegalArgumentException("Fator Rh invalido: " + texto);
    }
}
