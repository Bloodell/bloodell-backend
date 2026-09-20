package br.edu.cesar.bloodell.compartilhado.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class TipoSanguineo {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_abo", nullable = false, length = 2)
    private Abo abo;

    @Convert(converter = RhConverter.class)
    @Column(name = "tipo_rh", nullable = false, length = 1)
    private Rh rh;

    protected TipoSanguineo() {
    }

    public TipoSanguineo(Abo abo, Rh rh) {
        if (abo == null || rh == null) {
            throw new IllegalArgumentException("Tipo sanguineo exige grupo ABO e fator Rh.");
        }
        this.abo = abo;
        this.rh = rh;
    }

    public static TipoSanguineo de(String abo, String rh) {
        return new TipoSanguineo(Abo.doTexto(abo), Rh.doTexto(rh));
    }

    public static TipoSanguineo doTextoCompacto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Tipo sanguineo nao informado.");
        }
        String limpo = texto.trim().toUpperCase();
        String sinal = limpo.substring(limpo.length() - 1);
        String grupo = limpo.substring(0, limpo.length() - 1);
        return de(grupo, sinal);
    }

    public Abo getAbo() {
        return abo;
    }

    public Rh getRh() {
        return rh;
    }

    public String formatado() {
        return abo.getSigla() + rh.getSinal();
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof TipoSanguineo)) {
            return false;
        }
        TipoSanguineo tipo = (TipoSanguineo) outro;
        return abo == tipo.abo && rh == tipo.rh;
    }

    @Override
    public int hashCode() {
        return Objects.hash(abo, rh);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
