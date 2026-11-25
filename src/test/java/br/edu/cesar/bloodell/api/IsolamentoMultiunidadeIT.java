package br.edu.cesar.bloodell.api;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.autenticacao.dominio.*;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.telemetria.infraestrutura.ProcessadorDeTelemetria;
import java.security.SecureRandom;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "bloodell.seguranca.autenticacao-habilitada=true")
class IsolamentoMultiunidadeIT extends TesteDeIntegracao {
    private static final String SENHA = "Senha sintetica multiunidade!";
    private static final String SEGREDO;
    static {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        SEGREDO = Base64.getEncoder().encodeToString(bytes);
    }
    @DynamicPropertySource static void jwt(DynamicPropertyRegistry r) {
        r.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;
    @Autowired ProcessadorDeTelemetria processador;
    private String tokenA, tokenB, tokenAdmin, tokenSemVinculo;
    private Long usuarioA;

    @BeforeEach void preparar() throws Exception {
        assertThat(processador.aguardarFilaVazia(10_000)).isTrue();
        limparBanco(jdbc);
        for (int i = 1; i <= 4; i++) {
            jdbc.update("""
                    INSERT INTO unidades(id,tipo,nome,cidade,uf,latitude,longitude)
                    VALUES (?,?,?,?,?,-8.05,-34.88)
                    """, i, i <= 2 ? "HOSPITAL" : "HEMOCENTRO", "Unidade " + i, "Recife", "PE");
        }
        for (int i = 1; i <= 2; i++) {
            jdbc.update("""
                    INSERT INTO requisicoes(id,codigo,hospital_id,prioridade,status)
                    VALUES (?, ?, ?, 'ELETIVA', 'NA_FILA')
                    """, i, "REQ-" + i, i);
            jdbc.update("""
                    INSERT INTO itens_requisicao(requisicao_id,componente,tipo_abo,tipo_rh,quantidade)
                    VALUES (?, 'CONCENTRADO_HEMACIAS', 'O', '+', 1)
                    """, i);
            jdbc.update("""
                    INSERT INTO veiculos(id,placa,hemocentro_base_id,capacidade_bolsas,status,latitude,longitude)
                    VALUES (?, ?, ?, 20, 'DISPONIVEL', -8.05, -34.88)
                    """, i, "S02-" + i, i + 2);
            jdbc.update("""
                    INSERT INTO doacoes(id,codigo,hemocentro_id,data_hora,tipo_abo,tipo_rh,volume_ml)
                    VALUES (?, ?, ?, now(), 'O', '+', 450)
                    """, i, "DOA-" + i, i + 2);
            jdbc.update("""
                    INSERT INTO bolsas(id,codigo,tipo_abo,tipo_rh,componente,data_coleta,data_validade,
                        status,unidade_atual_id,doacao_id,sequencia_entrada)
                    VALUES (?, ?, 'O', '+', 'CONCENTRADO_HEMACIAS', current_date,
                        current_date + 30, 'DISPONIVEL', ?, ?, ?)
                    """, i, "BOL-" + i, i + 2, i, i);
            jdbc.update("""
                    INSERT INTO estoque_itens(unidade_id,tipo_abo,tipo_rh,componente,quantidade)
                    VALUES (?, 'O', '+', 'CONCENTRADO_HEMACIAS', 1)
                    """, i + 2);
            jdbc.update("""
                    INSERT INTO entregas(id,requisicao_id,veiculo_id,origem_id,status)
                    VALUES (?, ?, ?, ?, 'PLANEJADA')
                    """, i, i, i, i + 2);
            jdbc.update("""
                    INSERT INTO eventos_rastreabilidade(bolsa_id,status_novo,requisicao_id,descricao)
                    VALUES (?, 'DISPONIVEL', ?, ?)
                    """, i, i, "Evento unidade " + i);
            jdbc.update("""
                    INSERT INTO alertas(tipo,nivel,mensagem,veiculo_id,entrega_id,bolsa_id)
                    VALUES ('TESTE','INFO', ?, ?, ?, ?)
                    """, "Alerta " + i, i, i, i);
        }
        var a = conta("a", Perfil.LOGISTICA);
        var b = conta("b", Perfil.LOGISTICA);
        var admin = conta("admin", Perfil.ADMIN);
        var vazio = conta("vazio", Perfil.USUARIO_HOSPITAL);
        usuarioA = a.getId();
        tokenA = login(a); tokenB = login(b); tokenAdmin = login(admin); tokenSemVinculo = login(vazio);
        assertStatus(HttpMethod.PUT, "/usuarios/" + usuarioA + "/unidades",
                Map.of("unidadeIds", List.of(1, 3)), tokenAdmin, 200);
        assertStatus(HttpMethod.PUT, "/usuarios/" + b.getId() + "/unidades",
                Map.of("unidadeIds", List.of(2, 4)), tokenAdmin, 200);
    }

    @Test void paginacaoFiltraDadosETotaisEOmissaoDeFiltroNaoAmpliaEscopo() {
        var pagina = getMap("/requisicoes?size=1", tokenA);
        assertThat(((Number) pagina.get("totalElements")).longValue()).isEqualTo(1);
        assertThat(((Map<?, ?>) ((List<?>) pagina.get("content")).getFirst()).get("hospitalId")).isEqualTo(1);
        var bolsas = getMap("/bolsas?size=1", tokenA);
        assertThat(((Number) bolsas.get("totalElements")).longValue()).isEqualTo(1);
        assertThat(getMap("/requisicoes", tokenSemVinculo).get("content")).isEqualTo(List.of());
        assertThat(((Number) getMap("/requisicoes", tokenAdmin).get("totalElements")).longValue()).isEqualTo(2);
        assertStatus(HttpMethod.GET, "/requisicoes?hospitalId=2", null, tokenA, 403);
        assertStatus(HttpMethod.GET, "/bolsas?unidadeId=4", null, tokenA, 403);
        assertStatus(HttpMethod.GET, "/estoques?unidadeId=4", null, tokenA, 403);
    }

    @Test void idsDiretosNaoPermitemLeituraAlteracaoOuCancelamentoCruzados() {
        for (String rota : List.of("/requisicoes/2", "/bolsas/2", "/bolsas/2/rastreabilidade",
                "/doacoes/2", "/entregas/2", "/veiculos/2", "/hospitais/2", "/hemocentros/4",
                "/veiculos/2/telemetrias")) {
            assertStatus(HttpMethod.GET, rota, null, tokenA, 403);
        }
        assertStatus(HttpMethod.PUT, "/requisicoes/2", Map.of("prioridade", "URGENTE"), tokenA, 403);
        assertStatus(HttpMethod.DELETE, "/requisicoes/2", null, tokenA, 403);
        assertStatus(HttpMethod.DELETE, "/entregas/2", null, tokenA, 403);
        assertStatus(HttpMethod.POST, "/entregas/2/despacho", null, tokenA, 403);
        assertStatus(HttpMethod.DELETE, "/veiculos/2", null, tokenA, 403);
        assertStatus(HttpMethod.DELETE, "/hospitais/2", null, tokenA, 403);
        assertThat(jdbc.queryForObject("SELECT status FROM requisicoes WHERE id=2", String.class)).isEqualTo("NA_FILA");
        assertThat(jdbc.queryForObject("SELECT status FROM entregas WHERE id=2", String.class)).isEqualTo("PLANEJADA");
        assertStatus(HttpMethod.GET, "/requisicoes/2", null, tokenB, 200);
        assertStatus(HttpMethod.PUT, "/requisicoes/1", Map.of("prioridade", "URGENTE"), tokenA, 200);
        assertStatus(HttpMethod.DELETE, "/requisicoes/1", null, tokenA, 204);
    }

    @Test void projecoesIndicadoresEListasNaoRevelamOutraUnidade() {
        for (String rota : List.of("/doacoes", "/estoques", "/entregas", "/veiculos", "/alertas",
                "/hospitais", "/hemocentros", "/requisicoes/fila")) {
            var resposta = request(HttpMethod.GET, rota, null, tokenA, List.class);
            assertThat(resposta.getStatusCode().value()).as(rota).isEqualTo(200);
            assertThat(resposta.getBody()).as(rota).hasSize(1);
        }
        for (String rota : List.of("/algoritmos/fila", "/algoritmos/estoque", "/algoritmos/historico",
                "/indicadores/demanda", "/indicadores/estoque", "/indicadores/tempos")) {
            var resposta = request(HttpMethod.GET, rota, null, tokenA, String.class);
            assertThat(resposta.getStatusCode().value()).as(rota).isEqualTo(200);
            assertThat(resposta.getBody()).as(rota).doesNotContain("REQ-2", "BOL-2",
                    "\"hospital\":\"Unidade 2\"", "\"categoria\":\"Unidade 2\"");
        }
        assertStatus(HttpMethod.GET, "/algoritmos/estoque?unidadeId=4", null, tokenA, 403);
    }

    @Test void referenciasNoCorpoEAlocacaoNaoAlteramOutraUnidade() {
        Map<String, Object> pedido = Map.of("hospitalId", 2, "prioridade", "ELETIVA", "itens",
                List.of(Map.of("componente", "CONCENTRADO_HEMACIAS", "tipoSanguineo", "O+", "quantidade", 1)));
        assertStatus(HttpMethod.POST, "/requisicoes", pedido, tokenA, 403);
        // B pode acessar o hospital 2, mas nao o hemocentro 3 escolhido pela politica atual.
        assertStatus(HttpMethod.POST, "/requisicoes/2/alocacoes", null, tokenB, 403);
        assertThat(jdbc.queryForObject("SELECT status FROM requisicoes WHERE id=2", String.class)).isEqualTo("NA_FILA");
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque_itens WHERE unidade_id=3", Integer.class)).isEqualTo(1);
        assertStatus(HttpMethod.POST, "/requisicoes/1/alocacoes", null, tokenA, 201);
        assertThat(jdbc.queryForObject("SELECT status FROM bolsas WHERE id=1", String.class)).isEqualTo("ALOCADA");
        assertThat(jdbc.queryForObject("SELECT status FROM bolsas WHERE id=2", String.class)).isEqualTo("DISPONIVEL");
    }

    @Test void entregaETelemetriaExigemTodasAsUnidadesAntesDeQualquerEfeito() {
        jdbc.update("UPDATE entregas SET origem_id=4 WHERE id=1");
        assertStatus(HttpMethod.GET, "/entregas/1", null, tokenA, 403);
        assertStatus(HttpMethod.POST, "/entregas/1/despacho", null, tokenA, 403);
        var leitura = Map.of("veiculoId", 1, "entregaId", 1, "lat", -8.05, "lon", -34.88,
                "temperaturaC", 4.5, "statusVeiculo", "EM_ROTA");
        assertStatus(HttpMethod.POST, "/telemetrias", leitura, tokenA, 403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leituras_telemetria", Long.class)).isZero();
        assertStatus(HttpMethod.PUT, "/usuarios/" + usuarioA + "/unidades",
                Map.of("unidadeIds", List.of(1, 3, 4)), tokenAdmin, 200);
        assertStatus(HttpMethod.GET, "/entregas/1", null, tokenA, 200);
        assertStatus(HttpMethod.POST, "/telemetrias", leitura, tokenA, 202);
    }

    @Test void somenteAdminGerenciaVinculosERevogacaoValeComMesmoToken() {
        String rota = "/usuarios/" + usuarioA + "/unidades";
        assertStatus(HttpMethod.PUT, rota, Map.of("unidadeIds", List.of(1, 2, 3, 4)), tokenA, 403);
        assertStatus(HttpMethod.PUT, rota, Map.of("unidadeIds", List.of(1, 999999)), tokenAdmin, 404);
        assertStatus(HttpMethod.GET, "/requisicoes/1", null, tokenA, 200);
        assertStatus(HttpMethod.PUT, rota, Map.of("unidadeIds", List.of()), tokenAdmin, 200);
        assertStatus(HttpMethod.GET, "/requisicoes/1", null, tokenA, 403);
        assertStatus(HttpMethod.GET, "/requisicoes/1", null, tokenAdmin, 200);
    }

    private Usuario conta(String nome, Perfil perfil) {
        return usuarios.saveAndFlush(new Usuario(nome, nome + "@example.test", perfil, senhas.encode(SENHA)));
    }
    private String login(Usuario usuario) {
        var resposta = http.postForEntity("/api/v1/auth/login", Map.of("email", usuario.getEmail(), "senha", SENHA), Map.class);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        return (String) resposta.getBody().get("accessToken");
    }
    private Map getMap(String rota, String token) {
        var resposta = request(HttpMethod.GET, rota, null, token, Map.class);
        assertThat(resposta.getStatusCode().value()).as(rota).isEqualTo(200);
        return resposta.getBody();
    }
    private void assertStatus(HttpMethod metodo, String rota, Object corpo, String token, int status) {
        var resposta = request(metodo, rota, corpo, token, String.class);
        assertThat(resposta.getStatusCode().value()).as("%s %s: %s", metodo, rota, resposta.getBody()).isEqualTo(status);
    }
    private <T> ResponseEntity<T> request(HttpMethod metodo, String rota, Object corpo, String token, Class<T> tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange("/api/v1" + rota, metodo, new HttpEntity<>(corpo, headers), tipo);
    }
}
