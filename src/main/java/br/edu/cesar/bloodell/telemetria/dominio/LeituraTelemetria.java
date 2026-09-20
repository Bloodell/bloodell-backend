package br.edu.cesar.bloodell.telemetria.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "leituras_telemetria")
public class LeituraTelemetria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "entrega_id")
    private Long entregaId;

    @Embedded
    private CoordenadaGeo posicao;

    @Column(name = "temperatura_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperaturaCelsius;

    @Column(name = "status_veiculo", nullable = false, length = 20)
    private String statusVeiculo;

    @Column(name = "registrada_em", nullable = false)
    private OffsetDateTime registradaEm;

    @Column(name = "recebida_em", nullable = false)
    private OffsetDateTime recebidaEm;

    @Column(name = "correlacao", length = 40)
    private String correlacao;

    protected LeituraTelemetria() {
    }

    public LeituraTelemetria(Veiculo veiculo, Long entregaId, CoordenadaGeo posicao,
                             BigDecimal temperaturaCelsius, String statusVeiculo,
                             OffsetDateTime registradaEm, String correlacao) {
        this.veiculo = veiculo;
        this.entregaId = entregaId;
        this.posicao = posicao;
        this.temperaturaCelsius = temperaturaCelsius;
        this.statusVeiculo = statusVeiculo;
        this.registradaEm = registradaEm;
        this.recebidaEm = OffsetDateTime.now();
        this.correlacao = correlacao;
    }

    public Long getId() {
        return id;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public Long getEntregaId() {
        return entregaId;
    }

    public CoordenadaGeo getPosicao() {
        return posicao;
    }

    public BigDecimal getTemperaturaCelsius() {
        return temperaturaCelsius;
    }

    public String getStatusVeiculo() {
        return statusVeiculo;
    }

    public OffsetDateTime getRegistradaEm() {
        return registradaEm;
    }

    public OffsetDateTime getRecebidaEm() {
        return recebidaEm;
    }

    public String getCorrelacao() {
        return correlacao;
    }
}
