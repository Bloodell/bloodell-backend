package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "bloodell.seguranca.autenticacao-habilitada=true")
class AutenticacaoIT extends TesteDeIntegracao {

    private static final String SEGREDO = segredoAleatorio();
    private static final String SENHA = "Senha sintetica de teste!";
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder senhas;
    @Autowired private JwtEncoder emissor;
    @Autowired private JwtDecoder decoder;
    @Autowired private JdbcTemplate jdbc;
    private Usuario usuario;

    @DynamicPropertySource
    static void propriedadesJwt(DynamicPropertyRegistry registro) {
        registro.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO);
    }

    private static String segredoAleatorio() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @BeforeEach
    void prepararUsuario() {
        usuario = usuarios.saveAndFlush(new Usuario("Operador sintetico", UUID.randomUUID() + "@example.test",
                Perfil.OPERADOR_HEMOCENTRO, senhas.encode(SENHA)));
    }

    @Autowired
    private TestRestTemplate cliente;

    @Test
    @DisplayName("AUTH-01: consultar o usuario atual sem token retorna 401")
    void usuarioAtualExigeToken() {
        var resposta = cliente.getForEntity("/api/v1/auth/me", String.class);

        assertThat(resposta.getStatusCode())
                .as("a API deve exigir autenticacao para consultar o usuario atual")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginEmiteJwtComValidadeEConsultaIdentidade() {
        var resposta = login(usuario.getEmail().toUpperCase(java.util.Locale.ROOT), SENHA);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(resposta.getHeaders().get("Set-Cookie")).isNull();
        Map corpo = resposta.getBody();
        assertThat(corpo).containsEntry("tokenType", "Bearer").containsEntry("expiresIn", 900);
        assertThat((Map) corpo.get("usuario")).containsOnlyKeys("id", "nome", "email", "perfil");
        String token = (String) corpo.get("accessToken");
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("bloodell");
        assertThat(jwt.getAudience()).containsExactly("bloodell-api");
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(900));
        assertThat(jwt.getClaims()).doesNotContainKeys("senha", "senhaHash", "email");
        assertThat(usuario.getSenhaHash()).startsWith("$2a$12$").isNotEqualTo(SENHA);
        assertThat(senhas.matches(SENHA, usuario.getSenhaHash())).isTrue();
        var atual = me(token);
        assertThat(atual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atual.getBody()).containsEntry("email", usuario.getEmail())
                .containsEntry("perfil", "OPERADOR_HEMOCENTRO")
                .containsOnlyKeys("id", "nome", "email", "perfil");
        assertThat(atual.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void senhaIncorretaEContaInexistenteTemMesmaRespostaGenerica() {
        var incorreta = login(usuario.getEmail(), "senha errada");
        var inexistente = login("inexistente@example.test", SENHA);
        assertThat(incorreta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(incorreta.getBody().get("detail")).isEqualTo(inexistente.getBody().get("detail"));
        assertThat(incorreta.getBody()).doesNotContainKeys("senha", "senhaHash", "accessToken");
    }

    @Test
    void contaLegadaSemBcryptNaoEntra() {
        for (String hash : new String[] {null, "senha-em-texto-puro"}) {
            jdbc.update("UPDATE usuarios SET senha_hash = ? WHERE id = ?", hash, usuario.getId());
            assertThat(login(usuario.getEmail(), "senha-em-texto-puro").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void senhaMultibyteAcimaDoLimiteBcryptNaoCausaErroInterno() {
        assertThat(login(usuario.getEmail(), "á".repeat(50)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenMalformadoOuAssinadoPorOutraChaveRetorna401() {
        assertThat(me("nao-e-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var outraChave = new NimbusJwtEncoder(new ImmutableSecret<>(Base64.getDecoder().decode(segredoAleatorio())));
        String falso = outraChave.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims().build())).getTokenValue();
        assertThat(me(falso).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiradoEmissorAudienciaOuSubjectInvalidosRetornam401() {
        for (JwtClaimsSet claims : List.of(
                claims().expiresAt(Instant.now().minusSeconds(1)).build(),
                claims().issuer("outro-emissor").build(),
                claims().audience(List.of("outro-servico")).build(),
                claims().subject("nao-numerico").build(),
                claims().subject("9999999999999999999").build(),
                claims().notBefore(Instant.now().plusSeconds(300)).build())) {
            assertThat(me(assinar(claims)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void tokenSemExpiracaoRetorna401() {
        JwtClaimsSet semExpiracao = JwtClaimsSet.builder().issuer("bloodell").audience(List.of("bloodell-api"))
                .subject(usuario.getId().toString()).issuedAt(Instant.now()).build();
        assertThat(me(assinar(semExpiracao)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenNaQueryOuLoginHttpBasicNaoAutenticam() {
        String token = (String) login(usuario.getEmail(), SENHA).getBody().get("accessToken");
        assertThat(cliente.getForEntity("/api/v1/auth/me?access_token=" + token, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(usuario.getEmail(), SENHA);
        assertThat(cliente.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void contaRemovidaInvalidaAcessoEPerfilVemDoBanco() {
        String token = (String) login(usuario.getEmail(), SENHA).getBody().get("accessToken");
        jdbc.update("UPDATE usuarios SET perfil = 'LOGISTICA' WHERE id = ?", usuario.getId());
        assertThat(me(token).getBody()).containsEntry("perfil", "LOGISTICA");
        usuarios.deleteById(usuario.getId());
        assertThat(me(token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotasDeNegocioExigemIdentidadeESaudePermanecePublica() {
        assertThat(cliente.getForEntity("/api/v1/hemocentros", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(cliente.getForEntity("/actuator/health", Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) login(usuario.getEmail(), SENHA).getBody().get("accessToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        assertThat(cliente.exchange("/api/v1/hemocentros", HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void preflightCorsPermiteBearerApenasParaOrigemConfigurada() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setAccessControlRequestMethod(HttpMethod.GET);
        headers.setAccessControlRequestHeaders(List.of("authorization"));
        var resposta = cliente.exchange("/api/v1/auth/me", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
        headers.setOrigin("https://nao-permitido.example");
        assertThat(cliente.exchange("/api/v1/auth/me", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void bancoRejeitaEmailDuplicadoIgnorandoCaixaEEspacos() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO usuarios (nome,email,perfil) VALUES (?,?,?)", "Duplicado",
                " " + usuario.getEmail().toUpperCase(java.util.Locale.ROOT) + " ", "ADMIN"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return cliente.postForEntity("/api/v1/auth/login", Map.of("email", email, "senha", senha), Map.class);
    }

    private ResponseEntity<Map> me(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return cliente.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder().issuer("bloodell").audience(List.of("bloodell-api"))
                .subject(usuario.getId().toString()).issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(900));
    }

    private String assinar(JwtClaimsSet claims) {
        return emissor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
