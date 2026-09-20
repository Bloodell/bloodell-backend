package br.edu.cesar.bloodell.estoque.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "estoque_itens")
public class EstoqueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Embedded
    private TipoSanguineo tipoSanguineo;

    @Enumerated(EnumType.STRING)
    @Column(name = "componente", nullable = false, length = 30)
    private TipoComponente componente;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected EstoqueItem() {
    }

    public EstoqueItem(Unidade unidade, TipoSanguineo tipoSanguineo, TipoComponente componente) {
        this.unidade = unidade;
        this.tipoSanguineo = tipoSanguineo;
        this.componente = componente;
        this.quantidade = 0;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void somar(int valor) {
        int novo = this.quantidade + valor;
        if (novo < 0) {
            throw new IllegalStateException("Contagem de estoque nao pode ficar negativa para "
                    + tipoSanguineo.formatado() + "/" + componente);
        }
        this.quantidade = novo;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public TipoComponente getComponente() {
        return componente;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
