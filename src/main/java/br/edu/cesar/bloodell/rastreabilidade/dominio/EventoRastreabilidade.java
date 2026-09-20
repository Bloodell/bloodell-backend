package br.edu.cesar.bloodell.rastreabilidade.dominio;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
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
import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos_rastreabilidade")
public class EventoRastreabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false)
    private Bolsa bolsa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private EstadoBolsa statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private EstadoBolsa statusNovo;

    @Column(name = "requisicao_id")
    private Long requisicaoId;

    @Column(name = "entrega_id")
    private Long entregaId;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    protected EventoRastreabilidade() {
    }

    public EventoRastreabilidade(Bolsa bolsa, EstadoBolsa statusAnterior, EstadoBolsa statusNovo,
                                 Long requisicaoId, String descricao) {
        this.bolsa = bolsa;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.requisicaoId = requisicaoId;
        this.descricao = descricao;
        this.ocorridoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Bolsa getBolsa() {
        return bolsa;
    }

    public EstadoBolsa getStatusAnterior() {
        return statusAnterior;
    }

    public EstadoBolsa getStatusNovo() {
        return statusNovo;
    }

    public Long getRequisicaoId() {
        return requisicaoId;
    }

    public Long getEntregaId() {
        return entregaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
