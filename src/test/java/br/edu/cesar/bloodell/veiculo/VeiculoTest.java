package br.edu.cesar.bloodell.veiculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VeiculoTest {

    private Unidade hemocentro(String nome) {
        return new Hemocentro(nome,
                new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0578, -34.8829));
    }

    private Veiculo veiculo() {
        return new Veiculo("abc-1234", hemocentro("Hemocentro A"), 20, new CoordenadaGeo(-8.0578, -34.8829));
    }

    @Test
    @DisplayName("veiculo novo nasce disponivel e com a placa normalizada")
    void veiculoNovo() {
        Veiculo veiculo = veiculo();

        assertThat(veiculo.getPlaca()).isEqualTo("ABC-1234");
        assertThat(veiculo.getStatus()).isEqualTo(StatusVeiculo.DISPONIVEL);
    }

    @Test
    @DisplayName("veiculo parado pode ter placa, base, capacidade e posicao atualizadas")
    void atualizaCadastro() {
        Veiculo veiculo = veiculo();
        Unidade novaBase = hemocentro("Hemocentro B");

        veiculo.atualizarCadastro(" xyz-9876 ", novaBase, 35, new CoordenadaGeo(-8.1, -34.9));

        assertThat(veiculo.getPlaca()).isEqualTo("XYZ-9876");
        assertThat(veiculo.getHemocentroBase()).isSameAs(novaBase);
        assertThat(veiculo.getCapacidadeBolsas()).isEqualTo(35);
        assertThat(veiculo.getPosicaoAtual().getLatitude()).isEqualTo(-8.1);
    }

    @Test
    @DisplayName("veiculo em rota nao pode ter o cadastro alterado")
    void emRotaNaoAtualiza() {
        Veiculo veiculo = veiculo();
        veiculo.mudarStatus(StatusVeiculo.EM_ROTA);

        assertThatThrownBy(() -> veiculo.atualizarCadastro("XYZ-9876", hemocentro("Hemocentro B"), 35,
                new CoordenadaGeo(-8.1, -34.9)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("em rota");
    }

    @Test
    @DisplayName("capacidade precisa ser positiva tambem na atualizacao")
    void capacidadeInvalida() {
        Veiculo veiculo = veiculo();

        assertThatThrownBy(() -> veiculo.atualizarCadastro("ABC-1234", hemocentro("Hemocentro A"), 0,
                new CoordenadaGeo(-8.0578, -34.8829)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
