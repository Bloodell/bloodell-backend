package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    @Test
    void demonstracaoNaoPermiteCadastroAnonimoDeUsuarios() {
        var entrada = Map.of("nome", "Conta sintetica", "email", "anonimo@example.test",
                "perfil", "ADMIN", "senha", "Senha inicial sintetica!");
        assertThat(cliente.postForEntity("/api/v1/usuarios", entrada, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void demonstracaoNaoPermiteListarNemConsultarUsuarios() {
        for (String caminho : java.util.List.of("/api/v1/usuarios", "/api/v1/usuarios/1")) {
            assertThat(cliente.getForEntity(caminho, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void demonstracaoNaoPermiteAtualizarUsuario() throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create(cliente.getRootUri() + "/api/v1/usuarios/1"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"nome\":\"Nao permitido\"}")).build();
        try (var http = HttpClient.newHttpClient()) {
            assertThat(http.send(requisicao, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
        }
    }
}
