package br.edu.cesar.bloodell.doacao.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
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
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "doacoes")
public class Doacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_id", nullable = false)
    private Unidade hemocentro;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Embedded
    private TipoSanguineo tipoSanguineo;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    protected Doacao() {
    }

    public Doacao(String codigo, Unidade hemocentro, OffsetDateTime dataHora,
                  TipoSanguineo tipoSanguineo, int volumeMl) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Doacao precisa de codigo.");
        }
        if (volumeMl < 100 || volumeMl > 1000) {
            throw new IllegalArgumentException("Volume da doacao fora do intervalo plausivel (100 a 1000 ml).");
        }
        this.codigo = codigo.trim();
        this.hemocentro = Objects.requireNonNull(hemocentro, "Doacao precisa de hemocentro.");
        this.dataHora = Objects.requireNonNull(dataHora, "Doacao precisa de data e hora.");
        this.tipoSanguineo = Objects.requireNonNull(tipoSanguineo, "Doacao precisa de tipo sanguineo.");
        this.volumeMl = volumeMl;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Unidade getHemocentro() {
        return hemocentro;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public Integer getVolumeMl() {
        return volumeMl;
    }
}
