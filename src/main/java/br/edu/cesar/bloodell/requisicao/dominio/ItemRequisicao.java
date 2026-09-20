package br.edu.cesar.bloodell.requisicao.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
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

@Entity
@Table(name = "itens_requisicao")
public class ItemRequisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "componente", nullable = false, length = 30)
    private TipoComponente componente;

    @Embedded
    private TipoSanguineo tipoSolicitado;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "quantidade_alocada", nullable = false)
    private Integer quantidadeAlocada;

    protected ItemRequisicao() {
    }

    public ItemRequisicao(Requisicao requisicao, TipoComponente componente,
                          TipoSanguineo tipoSolicitado, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
        }
        this.requisicao = requisicao;
        this.componente = componente;
        this.tipoSolicitado = tipoSolicitado;
        this.quantidade = quantidade;
        this.quantidadeAlocada = 0;
    }

    public void registrarAlocacao(int quantas) {
        int novo = this.quantidadeAlocada + quantas;
        if (novo > quantidade) {
            throw new IllegalStateException("Alocado mais do que o solicitado no item " + id);
        }
        this.quantidadeAlocada = novo;
    }

    public boolean totalmenteAtendido() {
        return quantidadeAlocada.equals(quantidade);
    }

    public int faltando() {
        return quantidade - quantidadeAlocada;
    }

    public Long getId() {
        return id;
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public TipoComponente getComponente() {
        return componente;
    }

    public TipoSanguineo getTipoSolicitado() {
        return tipoSolicitado;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Integer getQuantidadeAlocada() {
        return quantidadeAlocada;
    }
}
