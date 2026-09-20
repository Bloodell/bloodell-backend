package br.edu.cesar.bloodell.bolsa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BolsaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 19);

    private Unidade hemocentro() {
        return new Hemocentro("Hemocentro de Teste",
                new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0578, -34.8829));
    }

    private Bolsa bolsaValida() {
        return new Bolsa("BL-TESTE-1", TipoSanguineo.doTextoCompacto("A+"),
                TipoComponente.CONCENTRADO_HEMACIAS, HOJE.minusDays(5), HOJE.plusDays(30),
                hemocentro(), 1L, 1L);
    }

    @Test
    @DisplayName("bolsa nasce RECEBIDA e chega a DISPONIVEL ao ser armazenada")
    void cicloInicial() {
        Bolsa bolsa = bolsaValida();
        assertThat(bolsa.getStatus()).isEqualTo(EstadoBolsa.RECEBIDA);

        bolsa.armazenar();

        assertThat(bolsa.getStatus()).isEqualTo(EstadoBolsa.DISPONIVEL);
    }

    @Test
    @DisplayName("validade e regra de operacao, nao so do banco")
    void validadeNaOperacao() {
        Bolsa vencida = new Bolsa("BL-TESTE-2", TipoSanguineo.doTextoCompacto("O+"),
                TipoComponente.PLAQUETAS, HOJE.minusDays(10), HOJE.minusDays(2),
                hemocentro(), 1L, 2L);
        vencida.armazenar();

        assertThat(vencida.estaDentroDaValidade(HOJE)).isFalse();
        assertThat(vencida.podeSerAlocada(HOJE)).isFalse();
        assertThatThrownBy(() -> vencida.reservarPara(HOJE))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("venceu");
    }

    @Test
    @DisplayName("bolsa que vence hoje ainda vale hoje")
    void venceHojeAindaVale() {
        Bolsa bolsa = new Bolsa("BL-TESTE-3", TipoSanguineo.doTextoCompacto("B-"),
                TipoComponente.CONCENTRADO_HEMACIAS, HOJE.minusDays(40), HOJE,
                hemocentro(), 1L, 3L);

        assertThat(bolsa.estaDentroDaValidade(HOJE)).isTrue();
    }

    @Test
    @DisplayName("transicao invalida e recusada com mensagem clara")
    void transicaoInvalida() {
        Bolsa bolsa = bolsaValida();

        assertThatThrownBy(() -> bolsa.mudarPara(EstadoBolsa.ENTREGUE))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Transicao invalida");
    }

    @Test
    @DisplayName("reserva pode ser desfeita, devolvendo a bolsa ao estoque")
    void reservaPodeSerLiberada() {
        Bolsa bolsa = bolsaValida();
        bolsa.armazenar();
        bolsa.reservarPara(HOJE);

        assertThat(bolsa.getStatus()).isEqualTo(EstadoBolsa.RESERVADA);

        bolsa.liberarReserva();

        assertThat(bolsa.getStatus()).isEqualTo(EstadoBolsa.DISPONIVEL);
    }

    @Test
    @DisplayName("validade menor ou igual a coleta e recusada na criacao")
    void validadeIncoerente() {
        assertThatThrownBy(() -> new Bolsa("BL-X", TipoSanguineo.doTextoCompacto("A+"),
                TipoComponente.PLASMA, HOJE, HOJE, hemocentro(), 1L, 4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }
}
