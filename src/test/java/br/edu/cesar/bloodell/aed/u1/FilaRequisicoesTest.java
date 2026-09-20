package br.edu.cesar.bloodell.aed.u1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilaRequisicoesTest {

    private static RequisicaoAed requisicao(String codigo, String prioridade, long chegada) {
        return new RequisicaoAed(codigo, "HospitalModelo", prioridade, chegada);
    }

    @Test
    @DisplayName("atende por prioridade e, dentro da prioridade, por ordem de chegada")
    void filaAtendePrioridadeDepoisChegada() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(requisicao("REQ-1", "ELETIVA", 1));
        fila.enfileirar(requisicao("REQ-2", "EMERGENCIA", 2));
        fila.enfileirar(requisicao("REQ-3", "URGENTE", 3));
        fila.enfileirar(requisicao("REQ-4", "EMERGENCIA", 4));

        assertThat(fila.desenfileirar().getCodigo()).isEqualTo("REQ-2");
        assertThat(fila.desenfileirar().getCodigo()).isEqualTo("REQ-4");
        assertThat(fila.desenfileirar().getCodigo()).isEqualTo("REQ-3");
        assertThat(fila.desenfileirar().getCodigo()).isEqualTo("REQ-1");
        assertThat(fila.desenfileirar()).isNull();
    }

    @Test
    @DisplayName("a listagem sai na mesma ordem em que sera atendida")
    void listagemRespeitaOrdemDeAtendimento() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(requisicao("REQ-1", "ELETIVA", 1));
        fila.enfileirar(requisicao("REQ-2", "EMERGENCIA", 2));
        fila.enfileirar(requisicao("REQ-3", "URGENTE", 3));

        RequisicaoAed[] ordem = fila.paraVetor();
        assertThat(ordem[0].getCodigo()).isEqualTo("REQ-2");
        assertThat(ordem[1].getCodigo()).isEqualTo("REQ-3");
        assertThat(ordem[2].getCodigo()).isEqualTo("REQ-1");
    }

    @Test
    @DisplayName("espiar mostra a proxima sem retirar da fila")
    void espiarNaoRetira() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(requisicao("REQ-1", "URGENTE", 1));

        assertThat(fila.espiar().getCodigo()).isEqualTo("REQ-1");
        assertThat(fila.tamanho()).isEqualTo(1);
    }

    @Test
    @DisplayName("fila vazia devolve nulo em vez de quebrar")
    void filaVaziaDevolveNulo() {
        FilaRequisicoes fila = new FilaRequisicoes();

        assertThat(fila.estaVazia()).isTrue();
        assertThat(fila.desenfileirar()).isNull();
        assertThat(fila.espiar()).isNull();
        assertThat(fila.paraVetor()).isEmpty();
    }

    @Test
    @DisplayName("prioridade desconhecida e recusada na hora de enfileirar")
    void prioridadeDesconhecida() {
        FilaRequisicoes fila = new FilaRequisicoes();

        assertThatThrownBy(() -> fila.enfileirar(requisicao("REQ-X", "QUANDO_DER", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prioridade desconhecida");
    }

    @Test
    @DisplayName("esvaziar e voltar a usar funciona (ponteiros de cauda religados)")
    void esvaziarEReusar() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(requisicao("REQ-1", "URGENTE", 1));
        fila.desenfileirar();
        fila.enfileirar(requisicao("REQ-2", "URGENTE", 2));

        assertThat(fila.tamanho()).isEqualTo(1);
        assertThat(fila.desenfileirar().getCodigo()).isEqualTo("REQ-2");
    }
}
