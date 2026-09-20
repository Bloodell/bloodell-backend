package br.edu.cesar.bloodell.unidade.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "unidades")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Embedded
    private Endereco endereco;

    @Embedded
    private CoordenadaGeo localizacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Unidade() {
    }

    protected Unidade(String nome, Endereco endereco, CoordenadaGeo localizacao) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Unidade precisa de nome.");
        }
        this.nome = nome.trim();
        this.endereco = Objects.requireNonNull(endereco, "Unidade precisa de endereco.");
        this.localizacao = Objects.requireNonNull(localizacao, "Unidade precisa de coordenada.");
        this.criadoEm = OffsetDateTime.now();
    }

    public abstract String tipo();

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public CoordenadaGeo getLocalizacao() {
        return localizacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void renomear(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("Nome da unidade nao pode ficar vazio.");
        }
        this.nome = novoNome.trim();
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Unidade)) {
            return false;
        }
        Unidade unidade = (Unidade) outro;
        return id != null && id.equals(unidade.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
