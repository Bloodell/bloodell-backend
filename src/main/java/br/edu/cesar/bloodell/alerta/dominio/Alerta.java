package br.edu.cesar.bloodell.alerta.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 15)
    private NivelAlerta nivel;

    @Column(name = "mensagem", nullable = false, length = 255)
    private String mensagem;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "entrega_id")
    private Long entregaId;

    @Column(name = "bolsa_id")
    private Long bolsaId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;

    protected Alerta() {
    }

    public Alerta(String tipo, NivelAlerta nivel, String mensagem, Long veiculoId, Long entregaId, Long bolsaId) {
        this.tipo = tipo;
        this.nivel = nivel;
        this.mensagem = mensagem;
        this.veiculoId = veiculoId;
        this.entregaId = entregaId;
        this.bolsaId = bolsaId;
        this.criadoEm = OffsetDateTime.now();
    }

    public void resolver() {
        this.resolvidoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public NivelAlerta getNivel() {
        return nivel;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public Long getEntregaId() {
        return entregaId;
    }

    public Long getBolsaId() {
        return bolsaId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getResolvidoEm() {
        return resolvidoEm;
    }
}
