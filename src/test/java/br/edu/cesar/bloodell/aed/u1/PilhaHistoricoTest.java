package br.edu.cesar.bloodell.aed.u1;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.aed.contratos.EventoAed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PilhaHistoricoTest {

    private static EventoAed evento(String codigoBolsa, String statusNovo) {
        return new EventoAed(codigoBolsa, null, statusNovo, "2026-09-19T10:00:00Z");
    }

    @Test
    @DisplayName("sai na ordem inversa da entrada (LIFO)")
    void pilhaEhLifo() {
        PilhaHistorico pilha = new PilhaHistorico();
        for (int indice = 1; indice <= 10; indice++) {
            pilha.empilhar(evento("BL-" + indice, "DISPONIVEL"));
        }

        assertThat(pilha.tamanho()).isEqualTo(10);
        for (int indice = 10; indice >= 1; indice--) {
            assertThat(pilha.desempilhar().getCodigoBolsa()).isEqualTo("BL-" + indice);
        }
        assertThat(pilha.estaVazia()).isTrue();
        assertThat(pilha.desempilhar()).isNull();
    }

    @Test
    @DisplayName("topo mostra o evento mais recente sem retirar")
    void topoNaoRetira() {
        PilhaHistorico pilha = new PilhaHistorico();
        pilha.empilhar(evento("BL-1", "RECEBIDA"));
        pilha.empilhar(evento("BL-1", "DISPONIVEL"));

        assertThat(pilha.topo().getStatusNovo()).isEqualTo("DISPONIVEL");
        assertThat(pilha.tamanho()).isEqualTo(2);
    }

    @Test
    @DisplayName("a listagem vai do mais recente para o mais antigo")
    void listagemDoMaisRecente() {
        PilhaHistorico pilha = new PilhaHistorico();
        pilha.empilhar(evento("BL-1", "RECEBIDA"));
        pilha.empilhar(evento("BL-1", "ARMAZENADA"));
        pilha.empilhar(evento("BL-1", "DISPONIVEL"));

        EventoAed[] eventos = pilha.paraVetor();
        assertThat(eventos[0].getStatusNovo()).isEqualTo("DISPONIVEL");
        assertThat(eventos[2].getStatusNovo()).isEqualTo("RECEBIDA");
    }

    @Test
    @DisplayName("pilha vazia devolve nulo em vez de quebrar")
    void pilhaVazia() {
        PilhaHistorico pilha = new PilhaHistorico();

        assertThat(pilha.estaVazia()).isTrue();
        assertThat(pilha.topo()).isNull();
        assertThat(pilha.desempilhar()).isNull();
        assertThat(pilha.paraVetor()).isEmpty();
    }
}
