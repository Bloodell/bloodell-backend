package br.edu.cesar.bloodell.entrega;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.entrega.dominio.Entrega;
import br.edu.cesar.bloodell.entrega.dominio.StatusEntrega;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Hospital;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntregaTest {

    private Unidade hemocentro() {
        return new Hemocentro("Hemocentro de Teste",
                new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0578, -34.8829));
    }

    private Unidade hospital() {
        return new Hospital("Hospital de Teste",
                new Endereco("Rua Hospital", "2", "Boa Vista", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0600, -34.8800));
    }

    private Entrega entregaPlanejada() {
        Unidade origem = hemocentro();
        Requisicao requisicao = new Requisicao("REQ-TESTE", hospital(), Prioridade.URGENTE);
        Veiculo veiculo = new Veiculo("ABC-1234", origem, 20, new CoordenadaGeo(-8.0578, -34.8829));
        return new Entrega(requisicao, veiculo, origem, "{\"paradas\":[]}",
                new BigDecimal("12.500"), 30);
    }

    @Test
    @DisplayName("entrega percorre o ciclo planejada, despachada, em transporte e entregue")
    void cicloDeEntrega() {
        Entrega entrega = entregaPlanejada();

        assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.PLANEJADA);
        entrega.mudarPara(StatusEntrega.DESPACHADA);
        entrega.mudarPara(StatusEntrega.EM_TRANSPORTE);
        entrega.mudarPara(StatusEntrega.ENTREGUE);

        assertThat(entrega.getIniciadaEm()).isNotNull();
        assertThat(entrega.getConcluidaEm()).isNotNull();
        assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.ENTREGUE);
    }

    @Test
    @DisplayName("entrega nao pode ser concluida sem entrar em transporte")
    void transicaoInvalida() {
        Entrega entrega = entregaPlanejada();

        assertThatThrownBy(() -> entrega.mudarPara(StatusEntrega.ENTREGUE))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Transicao invalida");
    }

    @Test
    @DisplayName("planejamento nao pode mudar depois do despacho")
    void planejamentoSoMudaAntesDoDespacho() {
        Entrega entrega = entregaPlanejada();
        entrega.mudarPara(StatusEntrega.DESPACHADA);

        assertThatThrownBy(() -> entrega.atualizarPlanejamento("[]", BigDecimal.ONE, 1))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("planejamento");
    }

    @Test
    @DisplayName("rota precisa ser representada por JSON")
    void rotaInvalida() {
        Unidade origem = hemocentro();
        Requisicao requisicao = new Requisicao("REQ-ROTA", hospital(), Prioridade.ELETIVA);
        Veiculo veiculo = new Veiculo("ABC-9999", origem, 20, new CoordenadaGeo(-8.0578, -34.8829));

        assertThatThrownBy(() -> new Entrega(requisicao, veiculo, origem, "rota textual", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");
    }
}
