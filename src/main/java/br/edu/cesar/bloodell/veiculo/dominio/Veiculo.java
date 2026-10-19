package br.edu.cesar.bloodell.veiculo.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
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
import java.util.Objects;

@Entity
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hemocentro_base_id", nullable = false)
    private Unidade hemocentroBase;

    @Column(name = "capacidade_bolsas", nullable = false)
    private Integer capacidadeBolsas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusVeiculo status;

    @Embedded
    private CoordenadaGeo posicaoAtual;

    protected Veiculo() {
    }

    public Veiculo(String placa, Unidade hemocentroBase, int capacidadeBolsas, CoordenadaGeo posicaoAtual) {
        this.placa = normalizarPlaca(placa);
        this.hemocentroBase = hemocentroBase;
        this.capacidadeBolsas = validarCapacidade(capacidadeBolsas);
        this.posicaoAtual = posicaoAtual;
        this.status = StatusVeiculo.DISPONIVEL;
    }

    /** Cadastro so pode mudar com o veiculo parado: em rota ele carrega uma entrega. */
    public void atualizarCadastro(String novaPlaca, Unidade novaBase, int novaCapacidade, CoordenadaGeo novaPosicao) {
        if (status == StatusVeiculo.EM_ROTA) {
            throw new RegraDeNegocioException("Veiculo " + placa + " esta em rota e nao pode ter o cadastro alterado.");
        }
        this.placa = normalizarPlaca(novaPlaca);
        this.hemocentroBase = Objects.requireNonNull(novaBase, "Veiculo precisa de hemocentro base.");
        this.capacidadeBolsas = validarCapacidade(novaCapacidade);
        this.posicaoAtual = Objects.requireNonNull(novaPosicao, "Veiculo precisa de posicao.");
    }

    public boolean emRota() {
        return status == StatusVeiculo.EM_ROTA;
    }

    private static String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Veiculo precisa de placa.");
        }
        return placa.trim().toUpperCase();
    }

    private static int validarCapacidade(int capacidadeBolsas) {
        if (capacidadeBolsas <= 0) {
            throw new IllegalArgumentException("Capacidade do veiculo deve ser maior que zero.");
        }
        return capacidadeBolsas;
    }

    public void atualizarPosicao(CoordenadaGeo novaPosicao) {
        this.posicaoAtual = novaPosicao;
    }

    public void mudarStatus(StatusVeiculo novoStatus) {
        this.status = novoStatus;
    }

    public Long getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public Unidade getHemocentroBase() {
        return hemocentroBase;
    }

    public Integer getCapacidadeBolsas() {
        return capacidadeBolsas;
    }

    public StatusVeiculo getStatus() {
        return status;
    }

    public CoordenadaGeo getPosicaoAtual() {
        return posicaoAtual;
    }
}
