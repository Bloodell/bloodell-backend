package br.edu.cesar.bloodell.compartilhado.dominio;

public enum Abo {
    A("A"),
    B("B"),
    AB("AB"),
    O("O");

    private final String sigla;

    Abo(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }

    public static Abo doTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Grupo ABO nao informado.");
        }
        for (Abo valor : values()) {
            if (valor.sigla.equalsIgnoreCase(texto.trim())) {
                return valor;
            }
        }
        throw new IllegalArgumentException("Grupo ABO invalido: " + texto);
    }
}
