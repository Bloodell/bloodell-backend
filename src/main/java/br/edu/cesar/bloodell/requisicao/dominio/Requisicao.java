package br.edu.cesar.bloodell.requisicao.dominio;

import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "requisicoes")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Unidade hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 15)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusRequisicao status;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "atendida_em")
    private OffsetDateTime atendidaEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemRequisicao> itens = new ArrayList<>();

    protected Requisicao() {
    }

    public Requisicao(String codigo, Unidade hospital, Prioridade prioridade) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Requisicao precisa de codigo.");
        }
        this.codigo = codigo.trim();
        this.hospital = Objects.requireNonNull(hospital, "Requisicao precisa de hospital.");
        this.prioridade = Objects.requireNonNull(prioridade, "Requisicao precisa de prioridade.");
        this.status = StatusRequisicao.CRIADA;
        this.criadaEm = OffsetDateTime.now();
    }

    public void adicionarItem(ItemRequisicao item) {
        Objects.requireNonNull(item, "Item nulo nao entra na requisicao.");
        this.itens.add(item);
    }

    public void mudarPara(StatusRequisicao novoStatus) {
        if (this.status == novoStatus) {
            return;
        }
        if (!this.status.podeIrPara(novoStatus)) {
            throw new RegraDeNegocioException("Transicao invalida para a requisicao " + codigo
                    + ": de " + status + " para " + novoStatus + ".");
        }
        this.status = novoStatus;
        if (novoStatus == StatusRequisicao.ATENDIDA) {
            this.atendidaEm = OffsetDateTime.now();
        }
    }

    public void entrarNaFila() {
        mudarPara(StatusRequisicao.NA_FILA);
    }

    public boolean totalmenteAtendida() {
        for (ItemRequisicao item : itens) {
            if (!item.totalmenteAtendido()) {
                return false;
            }
        }
        return !itens.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Unidade getHospital() {
        return hospital;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public OffsetDateTime getAtendidaEm() {
        return atendidaEm;
    }

    public List<ItemRequisicao> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public Long getVersao() {
        return versao;
    }
}
