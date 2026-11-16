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
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
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
class ConsultaDeUsuarioIT extends TesteDeIntegracao {
    private static final String SENHA = "Senha sintetica de consulta!";
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
    void adminListaUsuariosComPaginacaoPadraoSemCredenciais() {
        criarUsuarios(3);
        var resposta = consultar("/api/v1/usuarios", tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(resposta.getBody()).containsEntry("number", 0).containsEntry("size", 20)
                .containsEntry("totalElements", 4).containsEntry("totalPages", 1);
        var conteudo = conteudo(resposta);
        assertThat(conteudo).hasSize(4);
        conteudo.forEach(this::somenteDadosPublicosDaConta);
        assertThat(resposta.getBody().toString()).doesNotContain(SENHA, admin.getSenhaHash(), "senhaHash", "senha_hash");
    }

    @Test
    void paginasOrdenadasPorIdNaoRepetemOuOmitemContasComMesmoNome() {
        var esperados = new ArrayList<Long>();
        esperados.add(admin.getId());
        criarUsuarios(5).forEach(usuario -> esperados.add(usuario.getId()));
        var recebidos = new ArrayList<Long>();
        for (int pagina = 0; pagina < 3; pagina++) {
            var resposta = consultar("/api/v1/usuarios?page=" + pagina + "&size=2", tokenAdmin);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resposta.getBody()).containsEntry("number", pagina).containsEntry("size", 2)
                    .containsEntry("totalElements", 6).containsEntry("totalPages", 3);
            assertThat(conteudo(resposta)).hasSize(2);
            conteudo(resposta).forEach(usuario -> recebidos.add(((Number) usuario.get("id")).longValue()));
        }
        assertThat(recebidos).containsExactlyElementsOf(esperados).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(ints = {101, Integer.MAX_VALUE})
    void tamanhoDePaginaNuncaExcede100(int tamanhoPedido) {
        criarUsuarios(101);
        var resposta = consultar("/api/v1/usuarios?size=" + tamanhoPedido, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("size", 100).containsEntry("totalElements", 102);
        assertThat(conteudo(resposta)).hasSize(100);
        assertThat(conteudo(consultar("/api/v1/usuarios?page=1&size=" + tamanhoPedido, tokenAdmin))).hasSize(2);
    }

    @ParameterizedTest
    @CsvSource({"-3,0", "-1,-10"})
    void paginaNegativaETamanhoMenorQue1SeguemPadraoDaApi(int pagina, int tamanho) {
        var resposta = consultar("/api/v1/usuarios?page=" + pagina + "&size=" + tamanho, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("number", 0).containsEntry("size", 1);
    }

    @Test
    void paginaAlemDoFimRetorna200ComConteudoVazio() {
        var resposta = consultar("/api/v1/usuarios?page=999", tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(conteudo(resposta)).isEmpty();
        assertThat(resposta.getBody()).containsEntry("number", 999).containsEntry("totalElements", 1)
                .containsEntry("numberOfElements", 0);
    }

    @Test
    void adminConsultaContaPorIdSemExporSenha() {
        var equipe = criarUsuarios(1).getFirst();
        var resposta = consultar("/api/v1/usuarios/" + equipe.getId(), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        somenteDadosPublicosDaConta(resposta.getBody());
        assertThat(resposta.getBody()).containsEntry("email", equipe.getEmail()).containsEntry("perfil", "LOGISTICA");
        assertThat(resposta.getBody().toString()).doesNotContain(SENHA, equipe.getSenhaHash());
    }

    @Test
    void adminTambemPodeConsultarContaLegadaSemHash() {
        jdbc.update("INSERT INTO usuarios (nome,email,perfil) VALUES (?,?,?)", "Conta legada", "legada@example.test", "USUARIO_HOSPITAL");
        var legada = usuarios.buscarPorEmail("legada@example.test").orElseThrow();
        var resposta = consultar("/api/v1/usuarios/" + legada.getId(), tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        somenteDadosPublicosDaConta(resposta.getBody());
        assertThat(resposta.getBody()).containsEntry("email", "legada@example.test");
    }

    @Test
    void contaInexistenteRetorna404() {
        var resposta = consultar("/api/v1/usuarios/" + Long.MAX_VALUE, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("status", 404).containsKeys("type", "title", "detail", "instance");
    }

    @ParameterizedTest
    @ValueSource(strings = {"?page=abc", "?size=abc", "?page=2147483648", "?size=2147483648",
            "?page=2147483647&size=100", "/abc", "/9223372036854775808"})
    void parametrosNumericosInvalidosRetornam400(String sufixo) {
        var resposta = consultar("/api/v1/usuarios" + sufixo, tokenAdmin);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("status", 400);
        assertThat(resposta.getBody().toString()).doesNotContain(SENHA, admin.getSenhaHash());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void demaisPerfisNaoListamNemConsultamMesmoSeTokenAlegarAdmin(Perfil perfil) {
        var equipe = usuarios.saveAndFlush(new Usuario("Equipe", "equipe@example.test", perfil, admin.getSenhaHash()));
        var token = token(equipe, Instant.now().plusSeconds(900));
        for (String caminho : List.of("/api/v1/usuarios", "/api/v1/usuarios/" + admin.getId(), "/api/v1/usuarios/" + Long.MAX_VALUE)) {
            var resposta = consultar(caminho, token);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(resposta.getBody()).containsEntry("status", 403);
        }
    }

    @Test
    void consultasSemTokenComTokenInvalidoOuExpiradoRetornam401() {
        for (String token : new String[] {null, "token-invalido", token(admin, Instant.now().minusSeconds(1))}) {
            for (String caminho : List.of("/api/v1/usuarios", "/api/v1/usuarios/" + admin.getId())) {
                assertThat(consultar(caminho, token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @Test
    void swaggerDocumentaConsultasPaginacaoEExigenciaDeBearer() {
        var documento = cliente.getForEntity("/api/v1/openapi", Map.class);
        assertThat(documento.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map paths = (Map) documento.getBody().get("paths");
        Map listar = (Map) ((Map) paths.get("/api/v1/usuarios")).get("get");
        Map detalhar = (Map) ((Map) paths.get("/api/v1/usuarios/{id}")).get("get");
        assertThat((List) listar.get("security")).contains(Map.of("bearerAuth", List.of()));
        assertThat((List) detalhar.get("security")).contains(Map.of("bearerAuth", List.of()));
        assertThat((Map) listar.get("responses")).containsKeys("200", "400", "401", "403");
        assertThat((Map) detalhar.get("responses")).containsKeys("200", "400", "401", "403", "404");
        var nomes = ((List<Map>) listar.get("parameters")).stream().map(parametro -> parametro.get("name")).toList();
        assertThat(nomes).contains("page", "size");
    }

    private List<Usuario> criarUsuarios(int quantidade) {
        return usuarios.saveAllAndFlush(IntStream.range(0, quantidade).mapToObj(indice ->
                new Usuario("Equipe sintetica", "conta" + indice + "@example.test", Perfil.LOGISTICA, admin.getSenhaHash())).toList());
    }

    private ResponseEntity<Map> consultar(String caminho, String token) {
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return cliente.exchange(caminho, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private List<Map> conteudo(ResponseEntity<Map> resposta) { return (List<Map>) resposta.getBody().get("content"); }

    private void somenteDadosPublicosDaConta(Map usuario) { assertThat(usuario).containsOnlyKeys("id", "nome", "email", "perfil"); }

    private String token(Usuario usuario, Instant expiracao) {
        var claims = JwtClaimsSet.builder().issuer("bloodell").audience(List.of("bloodell-api"))
                .subject(usuario.getId().toString()).issuedAt(Instant.now().minusSeconds(60)).expiresAt(expiracao)
                .claim("perfil", "ADMIN").build();
        return emissor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
