package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Jornada de aceitação da Release 1: cada leitura consulta o PostgreSQL real. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureTestRestTemplate
class HistoriasReleaseUmIT extends TesteDeIntegracao {

    @Autowired private TestRestTemplate cliente;
    private static Long hemocentroId;
    private static Long hospitalId;
    private static Long bolsaId;
    private static Long requisicaoEmergencialId;

    @BeforeAll
    static void prepararBanco(@Autowired JdbcTemplate jdbc) {
        limparBanco(jdbc);
    }

    @Test
    @Order(1)
    @DisplayName("US-R1-01: doacao gera bolsa disponivel consultavel; componente duplicado e recusado")
    void registrarDoacaoEDisponibilizarBolsa() {
        hemocentroId = criarUnidade("hemocentros");
        hospitalId = criarUnidade("hospitais");
        Map<String, Object> entrada = Map.of("hemocentroId", hemocentroId,
                "tipoSanguineo", "A+", "volumeMl", 450, "componentes", List.of("PLASMA"));

        ResponseEntity<Map> criada = cliente.postForEntity("/api/v1/doacoes", entrada, Map.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long doacaoId = ((Number) criada.getBody().get("id")).longValue();
        List<Map<String, Object>> geradas = (List<Map<String, Object>>) criada.getBody().get("bolsasGeradas");
        assertThat(geradas).hasSize(1);
        bolsaId = ((Number) geradas.get(0).get("id")).longValue();

        assertThat(cliente.getForObject("/api/v1/doacoes/" + doacaoId, Map.class).get("bolsasGeradas"))
                .as("a consulta e feita apos a gravacao").isInstanceOf(List.class);
        Map<String, Object> bolsa = cliente.getForObject("/api/v1/bolsas/" + bolsaId, Map.class);
        assertThat(bolsa).containsEntry("status", "DISPONIVEL").containsEntry("componente", "PLASMA");

        Map<String, Object> repetida = Map.of("hemocentroId", hemocentroId,
                "tipoSanguineo", "A+", "volumeMl", 450,
                "componentes", List.of("PLASMA", "PLASMA"));
        assertThat(cliente.postForEntity("/api/v1/doacoes", repetida, Map.class).getStatusCode().value())
                .isEqualTo(422);
    }

    @Test
    @Order(2)
    @DisplayName("US-R1-02: requisicoes persistidas aparecem por prioridade e FIFO no empate")
    void criarEConsultarRequisicoesPrioritarias() {
        Map<String, Object> urgente1 = criarRequisicao("URGENTE");
        Map<String, Object> emergencia = criarRequisicao("EMERGENCIA");
        Map<String, Object> urgente2 = criarRequisicao("URGENTE");
        requisicaoEmergencialId = ((Number) emergencia.get("id")).longValue();

        List<Map<String, Object>> fila = cliente.getForObject("/api/v1/requisicoes/fila", List.class);
        assertThat(fila).extracting(item -> item.get("codigo"))
                .containsExactly(emergencia.get("codigo"), urgente1.get("codigo"), urgente2.get("codigo"));
        assertThat(cliente.getForObject("/api/v1/requisicoes/" + requisicaoEmergencialId, Map.class))
                .containsEntry("status", "NA_FILA").containsEntry("prioridade", "EMERGENCIA");
    }

    @Test
    @Order(3)
    @DisplayName("US-R1-03: alocacao provisoria U1 muda bolsa e requisicao persistidas")
    void processarAlocacaoProvisoria() {
        ResponseEntity<Map> resposta = cliente.postForEntity(
                "/api/v1/requisicoes/" + requisicaoEmergencialId + "/alocacoes", null, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("politicaAplicada", "PROVISORIA_U1")
                .containsEntry("quantidadeAlocada", 1);
        assertThat((List<?>) resposta.getBody().get("passos")).isNotEmpty();
        assertThat(cliente.getForObject("/api/v1/requisicoes/" + requisicaoEmergencialId, Map.class))
                .containsEntry("status", "ALOCADA");
        assertThat(cliente.getForObject("/api/v1/bolsas/" + bolsaId, Map.class))
                .containsEntry("status", "ALOCADA");
    }

    private Long criarUnidade(String tipo) {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/" + tipo, Map.of(
                "nome", "Unidade " + UUID.randomUUID(), "cidade", "Recife", "uf", "PE",
                "latitude", -8.05, "longitude", -34.88), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resposta.getBody().get("id")).longValue();
    }

    private Map<String, Object> criarRequisicao(String prioridade) {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/requisicoes", Map.of(
                "hospitalId", hospitalId, "prioridade", prioridade,
                "itens", List.of(Map.of("componente", "PLASMA", "tipoSanguineo", "A+", "quantidade", 1))), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody();
    }
}
