package br.edu.cesar.bloodell.compartilhado.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class Endereco {

    @Column(name = "logradouro", length = 160)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "bairro", length = 80)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 80)
    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "cep", length = 9)
    private String cep;

    protected Endereco() {
    }

    public Endereco(String logradouro, String numero, String bairro, String cidade, String uf, String cep) {
        if (cidade == null || cidade.isBlank()) {
            throw new IllegalArgumentException("Cidade e obrigatoria no endereco.");
        }
        if (uf == null || uf.trim().length() != 2) {
            throw new IllegalArgumentException("UF deve ter exatamente 2 letras.");
        }
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade.trim();
        this.uf = uf.trim().toUpperCase();
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getCep() {
        return cep;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Endereco)) {
            return false;
        }
        Endereco endereco = (Endereco) outro;
        return Objects.equals(logradouro, endereco.logradouro)
                && Objects.equals(numero, endereco.numero)
                && Objects.equals(bairro, endereco.bairro)
                && Objects.equals(cidade, endereco.cidade)
                && Objects.equals(uf, endereco.uf)
                && Objects.equals(cep, endereco.cep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logradouro, numero, bairro, cidade, uf, cep);
    }
}
