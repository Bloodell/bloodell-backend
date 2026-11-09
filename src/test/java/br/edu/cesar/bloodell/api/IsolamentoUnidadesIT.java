package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.autenticacao.dominio.*;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import java.security.SecureRandom;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "bloodell.seguranca.autenticacao-habilitada=true")
class IsolamentoUnidadesIT extends TesteDeIntegracao {
    static final String SEGREDO = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
    static final String SENHA = "Senha sintetica multiunidade!";
    @Autowired TestRestTemplate cliente;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void jwt(DynamicPropertyRegistry r) { r.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO); }

    @BeforeEach
    void preparar() { limparBanco(jdbc); }

    String token(Usuario usuario) {
        var resposta = cliente.postForEntity("/api/v1/auth/login",
                Map.of("email", usuario.getEmail(), "senha", SENHA), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("accessToken");
    }

    ResponseEntity<String> requisitar(String token, HttpMethod metodo, String caminho, Object corpo) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return cliente.exchange(caminho, metodo, new HttpEntity<>(corpo, h), String.class);
    }

    @Test
    void contaLegadaSemUnidadeNaoTemAcessoGlobal() {
        Usuario legado = usuarios.saveAndFlush(new Usuario("Legado", "legado@example.test",
                Perfil.USUARIO_HOSPITAL, senhas.encode(SENHA)));
        String token = token(legado);
        assertThat(requisitar(token, HttpMethod.GET, "/api/v1/hospitais", null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(requisitar(token, HttpMethod.GET, "/api/v1/auth/me", null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
