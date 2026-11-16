package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "bloodell.seguranca.autenticacao-habilitada=true")
@ExtendWith(OutputCaptureExtension.class)
class AtualizacaoDeUsuarioIT extends TesteDeIntegracao {
    private static final String SENHA = "Senha sintetica de atualizacao!";
    private static final String SEGREDO = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    @Autowired private TestRestTemplate cliente;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder senhas;
    @Autowired private JwtEncoder emissor;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlatformTransactionManager transacoes;
    private Usuario admin;
    private Usuario equipe;
    private String tokenAdmin;

    @DynamicPropertySource
    static void configurarJwt(DynamicPropertyRegistry registro) {
        registro.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO);
    }

    @BeforeEach
    void preparar() {
        limparBanco(jdbc);
        admin = usuarios.saveAndFlush(new Usuario("Admin sintetico", "admin@example.test", Perfil.ADMIN, senhas.encode(SENHA)));
        equipe = criar("equipe@example.test", Perfil.LOGISTICA);
        tokenAdmin = token(admin, Instant.now().plusSeconds(900));
    }

    @Test
    void adminAtualizaCadastroSemMudarIdOuSenha() throws Exception {
        var resposta = atualizar(equipe.getId(), Map.of("nome", "  Equipe atualizada  ",
                "email", "  NOVO@EXAMPLE.TEST  ", "perfil", "USUARIO_HOSPITAL"), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(resposta.getBody()).containsOnlyKeys("id", "nome", "email", "perfil")
                .containsEntry("nome", "Equipe atualizada").containsEntry("email", "novo@example.test")
                .containsEntry("perfil", "USUARIO_HOSPITAL");
        assertThat(((Number) resposta.getBody().get("id")).longValue()).isEqualTo(equipe.getId());
        var salva = usuarios.findById(equipe.getId()).orElseThrow();
        assertThat(salva.getSenhaHash()).isEqualTo(equipe.getSenhaHash());
        assertThat(resposta.getBody().toString()).doesNotContain(SENHA, salva.getSenhaHash());
    }

    @Test
    void patchParcialMantemCamposOmitidosEAceitaProprioEmail() throws Exception {
        assertThat(atualizar(equipe.getId(), Map.of("nome", "Nome corrigido"), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.OK);
        var parcial = usuarios.findById(equipe.getId()).orElseThrow();
        assertThat(parcial.getNome()).isEqualTo("Nome corrigido");
        assertThat(parcial.getEmail()).isEqualTo(equipe.getEmail());
        assertThat(parcial.getPerfil()).isEqualTo(equipe.getPerfil());
        assertThat(atualizar(equipe.getId(), Map.of("email", "  EQUIPE@EXAMPLE.TEST  "), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void novoEmailAutenticaEIdentidadeDoTokenAnteriorRefleteBanco() throws Exception {
        String anterior = (String) login(equipe.getEmail()).getBody().get("accessToken");
        assertThat(atualizar(equipe.getId(), Map.of("email", "novo@example.test", "perfil", "ADMIN"), tokenAdmin).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(login(equipe.getEmail()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var loginNovo = login("novo@example.test");
        assertThat(loginNovo.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (String token : List.of(anterior, (String) loginNovo.getBody().get("accessToken"))) {
            var headers = new HttpHeaders();
            headers.setBearerAuth(token);
            var me = cliente.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(me.getBody()).containsEntry("email", "novo@example.test").containsEntry("perfil", "ADMIN");
            assertThat(cliente.exchange("/api/v1/usuarios", HttpMethod.GET, new HttpEntity<>(headers), Map.class).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void perfilRebaixadoPerdePermissaoComMesmoToken() throws Exception {
        var segundo = criar("segundo-admin@example.test", Perfil.ADMIN);
        String tokenAnterior = token(segundo, Instant.now().plusSeconds(900));
        assertThat(atualizar(segundo.getId(), Map.of("perfil", "LOGISTICA"), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atualizar(equipe.getId(), Map.of("nome", "Nao permitido"), tokenAnterior).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void ultimoAdminNaoPodeSerRebaixadoENaoHaAtualizacaoParcial() throws Exception {
        var resposta = atualizar(admin.getId(), Map.of("nome", "Nao deve salvar", "perfil", "LOGISTICA"), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("status", 409);
        var salva = usuarios.findById(admin.getId()).orElseThrow();
        assertThat(salva.getNome()).isEqualTo(admin.getNome());
        assertThat(salva.getPerfil()).isEqualTo(Perfil.ADMIN);
    }

    @Test
    void adminLegadoSemHashNaoSubstituiUltimoAdminQuePodeEntrar() throws Exception {
        jdbc.update("INSERT INTO usuarios (nome,email,perfil) VALUES (?,?,?)", "Admin legado", "legado@example.test", "ADMIN");
        assertThat(atualizar(admin.getId(), Map.of("perfil", "LOGISTICA"), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void contaLegadaPodeTerDadosCorrigidosSemCriarOuTrocarSenha() throws Exception {
        jdbc.update("INSERT INTO usuarios (nome,email,perfil) VALUES (?,?,?)", "Conta legada", "legada@example.test", "LOGISTICA");
        var legada = usuarios.buscarPorEmail("legada@example.test").orElseThrow();
        assertThat(atualizar(legada.getId(), Map.of("nome", "Legada corrigida"), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(usuarios.findById(legada.getId()).orElseThrow().getSenhaHash()).isNull();
    }

    @Test
    void emailDeOutraContaRetorna409SemVazarCredenciais(CapturedOutput logs) throws Exception {
        var resposta = atualizar(equipe.getId(), Map.of("nome", "Nao deve salvar", "email", "  ADMIN@EXAMPLE.TEST  "), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().toString()).doesNotContain(SENHA, equipe.getSenhaHash());
        assertThat(usuarios.findById(equipe.getId()).orElseThrow().getNome()).isEqualTo(equipe.getNome());
        assertThat(logs.getAll()).doesNotContain(SENHA, equipe.getSenhaHash());
    }

    static Stream<Map<String, Object>> entradasInvalidas() {
        var nulo = new HashMap<String, Object>();
        nulo.put("nome", null);
        var emailNulo = new HashMap<String, Object>();
        emailNulo.put("email", null);
        var perfilNulo = new HashMap<String, Object>();
        perfilNulo.put("perfil", null);
        return Stream.of(Map.of(), Map.of("nome", "   "), Map.of("nome", "n".repeat(121)), Map.of("email", "invalido"),
                Map.of("email", "a".repeat(150) + "@example.test"), Map.of("perfil", "INVALIDO"),
                Map.of("senha", "Senha que nao pode alterar!"), Map.of("senha_hash", "hash-injetado"), Map.of("id", 123),
                nulo, emailNulo, perfilNulo);
    }

    @ParameterizedTest
    @MethodSource("entradasInvalidas")
    void camposInvalidosOuNaoPermitidosRetornam400SemMudarConta(Map<String, Object> entrada) throws Exception {
        var resposta = atualizar(equipe.getId(), entrada, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("status", 400);
        var salva = usuarios.findById(equipe.getId()).orElseThrow();
        assertThat(salva.getNome()).isEqualTo(equipe.getNome());
        assertThat(salva.getEmail()).isEqualTo(equipe.getEmail());
        assertThat(salva.getPerfil()).isEqualTo(equipe.getPerfil());
        assertThat(salva.getSenhaHash()).isEqualTo(equipe.getSenhaHash());
    }

    @Test
    void contaInexistenteRetorna404() throws Exception {
        assertThat(atualizar(Long.MAX_VALUE, Map.of("nome", "Inexistente"), tokenAdmin).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void demaisPerfisNaoAtualizamMesmoComAlegacaoAdmin(Perfil perfil) throws Exception {
        var operador = criar("operador@example.test", perfil);
        assertThat(atualizar(equipe.getId(), Map.of("nome", "Nao permitido"), token(operador, Instant.now().plusSeconds(900)))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenAusenteInvalidoOuExpiradoRetorna401() throws Exception {
        for (String token : new String[] {null, "token-invalido", token(admin, Instant.now().minusSeconds(1))}) {
            assertThat(atualizar(equipe.getId(), Map.of("nome", "Nao permitido"), token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void rebaixamentosSimultaneosPreservamUmAdmin() throws Exception {
        var segundo = criar("segundo-admin@example.test", Perfil.ADMIN);
        String tokenSegundo = token(segundo, Instant.now().plusSeconds(900));
        CyclicBarrier largada = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return atualizar(admin.getId(), Map.of("perfil", "LOGISTICA"), tokenAdmin).getStatusCode().value();
            });
            var segunda = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return atualizar(segundo.getId(), Map.of("perfil", "LOGISTICA"), tokenSegundo).getStatusCode().value();
            });
            assertThat(List.of(primeira.get(40, TimeUnit.SECONDS), segunda.get(40, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE perfil = 'ADMIN'", Long.class)).isEqualTo(1L);
    }

    @Test
    void atualizacoesConcorrentesDoMesmoEmailNaoCriamDuplicatas() throws Exception {
        var outra = criar("outra@example.test", Perfil.LOGISTICA);
        CyclicBarrier largada = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return atualizar(equipe.getId(), Map.of("email", "disputa@example.test"), tokenAdmin).getStatusCode().value();
            });
            var segunda = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return atualizar(outra.getId(), Map.of("email", "  DISPUTA@EXAMPLE.TEST  "), tokenAdmin).getStatusCode().value();
            });
            assertThat(List.of(primeira.get(40, TimeUnit.SECONDS), segunda.get(40, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE email = 'disputa@example.test'", Long.class)).isEqualTo(1L);
    }

    @Test
    void administradorRebaixadoEnquantoAguardaEscritaRecebe403() throws Exception {
        var segundo = criar("segundo-admin@example.test", Perfil.ADMIN);
        String tokenSegundo = token(segundo, Instant.now().plusSeconds(900));
        var pedido = new AtomicReference<Future<ResponseEntity<Map>>>();
        try (var executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transacoes).executeWithoutResult(status -> {
                jdbc.execute("LOCK TABLE usuarios IN SHARE ROW EXCLUSIVE MODE");
                pedido.set(executor.submit(() -> atualizar(equipe.getId(), Map.of("nome", "Nao deve salvar"), tokenSegundo)));
                try { aguardarEscritaBloqueada(); }
                catch (Exception erro) { throw new IllegalStateException("A requisicao nao aguardou o bloqueio.", erro); }
                jdbc.update("UPDATE usuarios SET perfil = 'LOGISTICA' WHERE id = ?", segundo.getId());
            });
            var resposta = pedido.get().get(40, TimeUnit.SECONDS);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(resposta.getBody()).containsEntry("status", 403);
        }
        assertThat(usuarios.findById(equipe.getId()).orElseThrow().getNome()).isEqualTo(equipe.getNome());
    }

    @Test
    void cadastroEAtualizacaoConcorrentesDoMesmoEmailPreservamUnicidade(CapturedOutput logs) throws Exception {
        CyclicBarrier largada = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var cadastro = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                var headers = new HttpHeaders();
                headers.setBearerAuth(tokenAdmin);
                var entrada = Map.of("nome", "Nova conta", "email", "disputa@example.test", "perfil", "LOGISTICA", "senha", SENHA);
                return cliente.exchange("/api/v1/usuarios", HttpMethod.POST, new HttpEntity<>(entrada, headers), Map.class).getStatusCode().value();
            });
            var atualizacao = executor.submit(() -> {
                largada.await(10, TimeUnit.SECONDS);
                return atualizar(equipe.getId(), Map.of("email", "  DISPUTA@EXAMPLE.TEST  "), tokenAdmin).getStatusCode().value();
            });
            int statusCadastro = cadastro.get(40, TimeUnit.SECONDS);
            int statusAtualizacao = atualizacao.get(40, TimeUnit.SECONDS);
            if (statusCadastro == 201) assertThat(statusAtualizacao).isEqualTo(409);
            else {
                assertThat(statusCadastro).isEqualTo(409);
                assertThat(statusAtualizacao).isEqualTo(200);
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE email = 'disputa@example.test'", Long.class)).isEqualTo(1L);
        assertThat(logs.getAll()).doesNotContain(SENHA, usuarios.buscarPorEmail("disputa@example.test").orElseThrow().getSenhaHash());
    }

    @Test
    void swaggerDocumentaPatchSemCampoDeSenha() {
        var documento = cliente.getForEntity("/api/v1/openapi", Map.class);
        Map paths = (Map) documento.getBody().get("paths");
        Map operacao = (Map) ((Map) paths.get("/api/v1/usuarios/{id}")).get("patch");
        assertThat((List) operacao.get("security")).contains(Map.of("bearerAuth", List.of()));
        assertThat((Map) operacao.get("responses")).containsKeys("200", "400", "401", "403", "404", "409");
        Map conteudo = (Map) ((Map) operacao.get("requestBody")).get("content");
        Map schema = (Map) ((Map) conteudo.get("application/json")).get("schema");
        String referencia = (String) schema.get("$ref");
        Map schemas = (Map) ((Map) documento.getBody().get("components")).get("schemas");
        Map atualizacao = (Map) schemas.get(referencia.substring(referencia.lastIndexOf('/') + 1));
        assertThat((Map) atualizacao.get("properties")).containsOnlyKeys("nome", "email", "perfil");
    }

    private Usuario criar(String email, Perfil perfil) {
        return usuarios.saveAndFlush(new Usuario("Equipe sintetica", email, perfil, admin.getSenhaHash()));
    }

    private void aguardarEscritaBloqueada() throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        // Conexao independente: observa o bloqueio sem reutilizar a transacao que o mantem.
        try (var conexao = jdbc.getDataSource().getConnection(); var consulta = conexao.prepareStatement("""
                SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                WHERE datname = current_database() AND wait_event_type = 'Lock' AND pid <> pg_backend_pid())
                """)) {
            do {
                try (var resultado = consulta.executeQuery()) {
                    resultado.next();
                    if (resultado.getBoolean(1)) return;
                }
                Thread.sleep(25);
            } while (System.nanoTime() < limite);
        }
        throw new IllegalStateException("Nenhuma escrita aguardando o bloqueio no banco de teste.");
    }

    private ResponseEntity<Map> atualizar(Long id, Map<String, Object> entrada, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(cliente.getRootUri() + "/api/v1/usuarios/" + id))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(entrada)));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        var resposta = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        var headers = new HttpHeaders();
        resposta.headers().map().forEach(headers::put);
        return ResponseEntity.status(resposta.statusCode()).headers(headers).body(mapper.readValue(resposta.body(), Map.class));
    }

    private ResponseEntity<Map> login(String email) {
        return cliente.postForEntity("/api/v1/auth/login", Map.of("email", email, "senha", SENHA), Map.class);
    }

    private String token(Usuario usuario, Instant expiracao) {
        var claims = JwtClaimsSet.builder().issuer("bloodell").audience(List.of("bloodell-api"))
                .subject(usuario.getId().toString()).issuedAt(Instant.now().minusSeconds(60)).expiresAt(expiracao)
                .claim("perfil", "ADMIN").build();
        return emissor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
