package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {"bloodell.seguranca.autenticacao-habilitada=false",
        "bloodell.seguranca.jwt.segredo-base64="})
class AutenticacaoDesabilitadaIT extends TesteDeIntegracao {
    @Autowired private TestRestTemplate cliente;

    @Test
    void modoDemonstrativoPreservaNegocioMasNuncaExpoeIdentidadeAnonima() {
        assertThat(cliente.getForEntity("/api/v1/hemocentros", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cliente.getForEntity("/api/v1/auth/me", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
