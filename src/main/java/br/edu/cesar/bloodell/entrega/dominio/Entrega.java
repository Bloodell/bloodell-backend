package br.edu.cesar.bloodell.entrega.dominio;

import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origem_id", nullable = false)
    private Unidade origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusEntrega status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rota", columnDefinition = "jsonb")
    private String rota;

    @Column(name = "distancia_km", precision = 10, scale = 3)
    private BigDecimal distanciaKm;

    @Column(name = "tempo_estimado_min")
    private Integer tempoEstimadoMin;

    @Column(name = "iniciada_em")
    private OffsetDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    protected Entrega() {
    }

    public Entrega(Requisicao requisicao, Veiculo veiculo, Unidade origem,
                   String rota, BigDecimal distanciaKm, Integer tempoEstimadoMin) {
        this.requisicao = Objects.requireNonNull(requisicao, "Entrega precisa de requisicao.");
        this.veiculo = Objects.requireNonNull(veiculo, "Entrega precisa de veiculo.");
        this.origem = Objects.requireNonNull(origem, "Entrega precisa de origem.");
        atualizarPlanejamento(rota, distanciaKm, tempoEstimadoMin);
        this.status = StatusEntrega.PLANEJADA;
    }

    public void atualizarPlanejamento(String novaRota, BigDecimal novaDistanciaKm, Integer novoTempoEstimadoMin) {
        if (status != null && status != StatusEntrega.PLANEJADA) {
            throw new RegraDeNegocioException("O planejamento so pode ser alterado enquanto a entrega estiver planejada.");
        }
        if (novaDistanciaKm != null && novaDistanciaKm.signum() < 0) {
            throw new IllegalArgumentException("Distancia da entrega nao pode ser negativa.");
        }
        if (novoTempoEstimadoMin != null && novoTempoEstimadoMin <= 0) {
            throw new IllegalArgumentException("Tempo estimado deve ser maior que zero.");
        }
        this.rota = normalizarRota(novaRota);
        this.distanciaKm = novaDistanciaKm;
        this.tempoEstimadoMin = novoTempoEstimadoMin;
    }

    public void mudarPara(StatusEntrega novoStatus) {
        if (!status.podeIrPara(novoStatus)) {
            throw new RegraDeNegocioException("Transicao invalida para a entrega " + id
                    + ": de " + status + " para " + novoStatus + ".");
        }
        this.status = novoStatus;
        if (novoStatus == StatusEntrega.EM_TRANSPORTE) {
            this.iniciadaEm = OffsetDateTime.now();
        }
        if (novoStatus == StatusEntrega.ENTREGUE) {
            this.concluidaEm = OffsetDateTime.now();
        }
    }

    private String normalizarRota(String novaRota) {
        if (novaRota == null || novaRota.isBlank()) {
            return null;
        }
        String rotaNormalizada = novaRota.trim();
        if (!(rotaNormalizada.startsWith("{") || rotaNormalizada.startsWith("["))) {
            throw new IllegalArgumentException("Rota deve ser um documento JSON.");
        }
        return rotaNormalizada;
    }

    public Long getId() { return id; }
    public Requisicao getRequisicao() { return requisicao; }
    public Veiculo getVeiculo() { return veiculo; }
    public Unidade getOrigem() { return origem; }
    public StatusEntrega getStatus() { return status; }
    public String getRota() { return rota; }
    public BigDecimal getDistanciaKm() { return distanciaKm; }
    public Integer getTempoEstimadoMin() { return tempoEstimadoMin; }
    public OffsetDateTime getIniciadaEm() { return iniciadaEm; }
    public OffsetDateTime getConcluidaEm() { return concluidaEm; }
}
