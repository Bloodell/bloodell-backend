package br.edu.cesar.bloodell.bolsa.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
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
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "bolsas")
public class Bolsa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Embedded
    private TipoSanguineo tipoSanguineo;

    @Enumerated(EnumType.STRING)
    @Column(name = "componente", nullable = false, length = 30)
    private TipoComponente componente;

    @Column(name = "data_coleta", nullable = false)
    private LocalDate dataColeta;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EstadoBolsa status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_atual_id", nullable = false)
    private Unidade unidadeAtual;

    @Column(name = "doacao_id")
    private Long doacaoId;

    @Column(name = "sequencia_entrada", nullable = false)
    private Long sequenciaEntrada;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Bolsa() {
    }

    public Bolsa(String codigo, TipoSanguineo tipoSanguineo, TipoComponente componente,
                 LocalDate dataColeta, LocalDate dataValidade, Unidade unidadeAtual,
                 Long doacaoId, long sequenciaEntrada) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Bolsa precisa de codigo.");
        }
        if (dataValidade == null || dataColeta == null || !dataValidade.isAfter(dataColeta)) {
            throw new IllegalArgumentException("Data de validade deve ser posterior a data de coleta.");
        }
        this.codigo = codigo.trim();
        this.tipoSanguineo = Objects.requireNonNull(tipoSanguineo, "Bolsa precisa de tipo sanguineo.");
        this.componente = Objects.requireNonNull(componente, "Bolsa precisa de componente.");
        this.dataColeta = dataColeta;
        this.dataValidade = dataValidade;
        this.unidadeAtual = Objects.requireNonNull(unidadeAtual, "Bolsa precisa de unidade atual.");
        this.doacaoId = doacaoId;
        this.sequenciaEntrada = sequenciaEntrada;
        this.status = EstadoBolsa.RECEBIDA;
        this.criadoEm = OffsetDateTime.now();
    }

    public boolean estaDentroDaValidade(LocalDate hoje) {
        return !dataValidade.isBefore(hoje);
    }

    public boolean podeSerAlocada(LocalDate hoje) {
        return status.disponivelParaAlocacao() && estaDentroDaValidade(hoje);
    }

    public void mudarPara(EstadoBolsa novoEstado) {
        if (this.status == novoEstado) {
            return;
        }
        if (!this.status.podeIrPara(novoEstado)) {
            throw new RegraDeNegocioException("Transicao invalida para a bolsa " + codigo
                    + ": de " + status + " para " + novoEstado + ".");
        }
        this.status = novoEstado;
    }

    public void armazenar() {
        mudarPara(EstadoBolsa.ARMAZENADA);
        mudarPara(EstadoBolsa.DISPONIVEL);
    }

    public void reservarPara(LocalDate hoje) {
        if (!estaDentroDaValidade(hoje)) {
            throw new RegraDeNegocioException("A bolsa " + codigo + " venceu em " + dataValidade
                    + " e nao pode ser reservada.");
        }
        mudarPara(EstadoBolsa.RESERVADA);
    }

    public void confirmarAlocacao() {
        mudarPara(EstadoBolsa.ALOCADA);
    }

    public void liberarReserva() {
        mudarPara(EstadoBolsa.DISPONIVEL);
    }

    public void marcarVencida() {
        this.status = EstadoBolsa.VENCIDA;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public TipoComponente getComponente() {
        return componente;
    }

    public LocalDate getDataColeta() {
        return dataColeta;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public EstadoBolsa getStatus() {
        return status;
    }

    public Unidade getUnidadeAtual() {
        return unidadeAtual;
    }

    public Long getDoacaoId() {
        return doacaoId;
    }

    public Long getSequenciaEntrada() {
        return sequenciaEntrada;
    }

    public Long getVersao() {
        return versao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Bolsa)) {
            return false;
        }
        Bolsa bolsa = (Bolsa) outro;
        return id != null && id.equals(bolsa.id);
    }

    @Override
    public int hashCode() {
        return Bolsa.class.hashCode();
    }
}
