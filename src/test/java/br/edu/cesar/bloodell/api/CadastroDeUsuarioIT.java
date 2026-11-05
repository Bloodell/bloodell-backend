package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "bloodell.seguranca.autenticacao-habilitada=true")
@ExtendWith(OutputCaptureExtension.class)
class CadastroDeUsuarioIT extends TesteDeIntegracao {
    private static final String SENHA = "Senha inicial sintetica!";
    private static final String SEGREDO = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
    @Autowired private TestRestTemplate cliente;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder senhas;
    @Autowired private JwtEncoder emissor;
    @Autowired private JdbcTemplate jdbc;
    private Usuario admin;
    private String tokenAdmin;

    @DynamicPropertySource
    static void configurarJwt(DynamicPropertyRegistry registro) {
        registro.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO);
    }

    @BeforeEach
    void preparar() {
        limparBanco(jdbc);
        admin = usuarios.saveAndFlush(new Usuario("Admin sintetico", "admin@example.test", Perfil.ADMIN, senhas.encode(SENHA)));
        tokenAdmin = token(admin, Instant.now().plusSeconds(900));
    }

    @Test
    void adminCadastraContaEContaEntra() {
        var entrada = entrada("  NOVA.CONTA@EXAMPLE.TEST  ", Perfil.OPERADOR_HEMOCENTRO);
        entrada.put("nome", "  Conta sintetica  ");
        var resposta = cadastrar(entrada, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(resposta.getHeaders().get("Set-Cookie")).isNull();
        assertThat(resposta.getBody()).containsOnlyKeys("id", "nome", "email", "perfil")
                .containsEntry("nome", "Conta sintetica").containsEntry("email", "nova.conta@example.test")
                .containsEntry("perfil", "OPERADOR_HEMOCENTRO");
        var salva = usuarios.buscarPorEmail("nova.conta@example.test").orElseThrow();
        assertThat(salva.getSenhaHash()).startsWith("$2a$12$").isNotEqualTo(SENHA);
        assertThat(senhas.matches(SENHA, salva.getSenhaHash())).isTrue();
        var login = cliente.postForEntity("/api/v1/auth/login", Map.of("email", salva.getEmail(), "senha", SENHA), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        var me = cliente.exchange("/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(cabecalhos((String) login.getBody().get("accessToken"))), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", salva.getEmail()).containsOnlyKeys("id", "nome", "email", "perfil");
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void adminPodeCadastrarCadaPerfilExistente(Perfil perfil) {
        var resposta = cadastrar(entrada("perfil@example.test", perfil), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("perfil", perfil.name());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void demaisPerfisNaoPodemCadastrarNemMesmoSeTokenAlegarAdmin(Perfil perfil) {
        var operador = usuarios.saveAndFlush(new Usuario("Equipe sintetica", "equipe@example.test", perfil, senhas.encode(SENHA)));
        var resposta = cadastrar(entrada("tentativa@example.test", Perfil.ADMIN), token(operador, Instant.now().plusSeconds(900)));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("status", 403).containsKeys("type", "title", "detail", "instance");
        assertThat(usuarios.buscarPorEmail("tentativa@example.test")).isEmpty();
    }

    @Test
    void semTokenTokenInvalidoOuExpiradoNaoCadastram() {
        for (String token : new String[] {null, "token-invalido", token(admin, Instant.now().minusSeconds(1))}) {
            assertThat(cadastrar(entrada("sem-acesso@example.test", Perfil.ADMIN), token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void perfilAtualDoBancoDefinePermissaoMesmoComTokenAntigo() {
        jdbc.update("UPDATE usuarios SET perfil = 'LOGISTICA' WHERE id = ?", admin.getId());
        assertThat(cadastrar(entrada("sem-permissao@example.test", Perfil.ADMIN), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void camposInvalidosRetornam400SemGravarOuExporSenha(CapturedOutput logs) {
        List<Map<String, Object>> entradas = new ArrayList<>();
        Map<String, List<Object>> invalidos = Map.of(
                "nome", List.of("", "   ", "n".repeat(121)),
                "email", List.of("", "email-invalido", "a".repeat(150) + "@example.test"),
                "perfil", List.of("DESCONHECIDO", "admin", ""),
                "senha", List.of("", " ".repeat(12), "a".repeat(11), "a".repeat(73), "á".repeat(37)));
        invalidos.forEach((campo, valores) -> valores.forEach(valor -> {
            var entrada = entrada("invalido@example.test", Perfil.ADMIN);
            entrada.put(campo, valor);
            entradas.add(entrada);
        }));
        for (String campo : List.of("nome", "email", "perfil", "senha")) {
            var entrada = entrada("invalido@example.test", Perfil.ADMIN);
            entrada.put(campo, null);
            entradas.add(entrada);
        }
        for (var entrada : entradas) {
            var resposta = cadastrar(entrada, tokenAdmin);
            assertThat(resposta.getStatusCode()).as("campo invalido: %s", entrada.keySet()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resposta.getBody()).containsEntry("status", 400).doesNotContainKeys("senha", "senhaHash", "senha_hash");
            assertThat(resposta.getBody().toString()).doesNotContain(SENHA);
        }
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(logs.getAll()).doesNotContain(SENHA);
    }

    static Stream<String> senhasNoLimite() { return Stream.of("a".repeat(12), "a".repeat(72), "á".repeat(36)); }

    @ParameterizedTest
    @MethodSource("senhasNoLimite")
    void senhaNosLimitesEhAceitaSemTruncamento(String senha) {
        var entrada = entrada("limite@example.test", Perfil.LOGISTICA);
        entrada.put("senha", senha);
        assertThat(cadastrar(entrada, tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var salva = usuarios.buscarPorEmail("limite@example.test").orElseThrow();
        assertThat(senhas.matches(senha, salva.getSenhaHash())).isTrue();
    }

    @Test
    void emailDuplicadoRetorna409SemVazarSenhaOuHashNosLogs(CapturedOutput logs) {
        assertThat(cadastrar(entrada("duplicado@example.test", Perfil.LOGISTICA), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var hash = usuarios.buscarPorEmail("duplicado@example.test").orElseThrow().getSenhaHash();
        for (String email : List.of("duplicado@example.test", "  DUPLICADO@EXAMPLE.TEST  ")) {
            var resposta = cadastrar(entrada(email, Perfil.ADMIN), tokenAdmin);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).containsEntry("status", 409);
            assertThat(resposta.getBody().toString()).doesNotContain(SENHA, hash);
        }
        assertThat(logs.getAll()).doesNotContain(SENHA, hash);
        assertThat(usuarios.count()).isEqualTo(2);
    }

    @Test
    void cadastrosConcorrentesDoMesmoEmailRetornam201E409() throws Exception {
        CyclicBarrier largada = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return cadastrar(entrada("concorrente@example.test", Perfil.LOGISTICA), tokenAdmin).getStatusCode().value();
            });
            var segunda = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return cadastrar(entrada("  CONCORRENTE@EXAMPLE.TEST  ", Perfil.ADMIN), tokenAdmin).getStatusCode().value();
            });
            assertThat(List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(usuarios.count()).isEqualTo(2);
    }

    @Test
    void swaggerDocumentaCadastroProtegidoESenhaSomenteParaEscrita() {
        var documento = cliente.getForEntity("/api/v1/openapi", Map.class);
        assertThat(documento.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map paths = (Map) documento.getBody().get("paths");
        Map operacao = (Map) ((Map) paths.get("/api/v1/usuarios")).get("post");
        assertThat((List) operacao.get("security")).contains(Map.of("bearerAuth", List.of()));
        assertThat((Map) operacao.get("responses")).containsKeys("201", "400", "401", "403", "409");
        Map conteudo = (Map) ((Map) operacao.get("requestBody")).get("content");
        Map schema = (Map) ((Map) conteudo.get("application/json")).get("schema");
        String referencia = (String) schema.get("$ref");
        Map schemas = (Map) ((Map) documento.getBody().get("components")).get("schemas");
        Map cadastro = (Map) schemas.get(referencia.substring(referencia.lastIndexOf('/') + 1));
        Map senha = (Map) ((Map) cadastro.get("properties")).get("senha");
        assertThat(senha).containsEntry("writeOnly", true);
    }

    private Map<String, Object> entrada(String email, Perfil perfil) {
        return new HashMap<>(Map.of("nome", "Conta sintetica", "email", email, "perfil", perfil.name(), "senha", SENHA));
    }

    private ResponseEntity<Map> cadastrar(Map<String, Object> entrada, String token) {
        return cliente.exchange("/api/v1/usuarios", HttpMethod.POST, new HttpEntity<>(entrada, cabecalhos(token)), Map.class);
    }

    private HttpHeaders cabecalhos(String token) {
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return headers;
    }

    private String token(Usuario usuario, Instant expiracao) {
        var claims = JwtClaimsSet.builder().issuer("bloodell").audience(List.of("bloodell-api"))
                .subject(usuario.getId().toString()).issuedAt(Instant.now().minusSeconds(60)).expiresAt(expiracao)
                .claim("perfil", "ADMIN").build();
        return emissor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
