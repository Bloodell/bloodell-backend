package br.edu.cesar.bloodell.compartilhado.apresentacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ManipuladorGlobalDeErrosTest {
    private final ManipuladorGlobalDeErros manipulador = new ManipuladorGlobalDeErros();

    @Test
    void violacaoDeIntegridadeRetorna409ComEnvelopeSemDetalheDoBanco() {
        var requisicao = new MockHttpServletRequest("POST", "/api/v1/veiculos");
        var resposta = manipulador.tratarIntegridade(
                new DataIntegrityViolationException("duplicate key value exposes database details"), requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().status()).isEqualTo(409);
        assertThat(resposta.getBody().type()).endsWith("/conflito-de-integridade");
        assertThat(resposta.getBody().instance()).isEqualTo("/api/v1/veiculos");
        assertThat(resposta.getBody().detail()).doesNotContain("duplicate key", "database");
    }

    @Test
    void preservaStatus4xxDeResponseStatusException() {
        var requisicao = new MockHttpServletRequest("POST", "/api/v1/veiculos");
        var resposta = manipulador.tratarStatusHttp(
                new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "metodo nao suportado"), requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().status()).isEqualTo(405);
        assertThat(resposta.getBody().type()).endsWith("/erro-http");
    }

}
