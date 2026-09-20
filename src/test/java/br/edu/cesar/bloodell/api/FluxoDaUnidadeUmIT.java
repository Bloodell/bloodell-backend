package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureTestRestTemplate
class FluxoDaUnidadeUmIT extends TesteDeIntegracao {

    @Autowired
    private TestRestTemplate cliente;

    @Test
    @Order(1)
    @DisplayName("health check responde antes de qualquer coisa")
    void healthCheck() {
        ResponseEntity<Map> resposta = cliente.getForEntity("/actuator/health", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "UP");
    }

    @Test
    @Order(2)
    @DisplayName("o cenario sintetico e criado com a semente 42")
    void criarCenario() {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/demonstracoes/cenarios",
                Map.of("cenario", "teste", "semente", 42), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat((Integer) corpo.get("bolsasGeradas")).isPositive();
        assertThat((Integer) corpo.get("requisicoesCriadas")).isEqualTo(7);
        assertThat((String) corpo.get("aviso")).contains("sintetico");
    }

    @Test
    @Order(3)
    @DisplayName("popular duas vezes responde 409 em vez de duplicar dados")
    void cenarioDuplicado() {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/demonstracoes/cenarios",
                Map.of("cenario", "teste", "semente", 42), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("status", 409);
        assertThat((String) resposta.getBody().get("type")).contains("/erros/conflito");
    }

    @Test
    @Order(4)
    @DisplayName("a fila sai ordenada por prioridade e depois por chegada")
    void filaRespeitaPrioridade() {
        ResponseEntity<List> resposta = cliente.getForEntity("/api/v1/requisicoes/fila", List.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> fila = resposta.getBody();
        assertThat(fila).isNotEmpty();

        List<String> ordemDasPrioridades = fila.stream()
                .map(item -> (String) item.get("prioridade")).toList();
        int ultimoPeso = 0;
        for (String prioridade : ordemDasPrioridades) {
            int peso = switch (prioridade) {
                case "EMERGENCIA" -> 1;
                case "URGENTE" -> 2;
                default -> 3;
            };
            assertThat(peso).isGreaterThanOrEqualTo(ultimoPeso);
            ultimoPeso = peso;
        }
    }

    @Test
    @Order(5)
    @DisplayName("o retrato da lista encadeada vem na ordem de chegada")
    void retratoDaLista() {
        ResponseEntity<Map> resposta = cliente.getForEntity("/api/v1/algoritmos/estoque", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> corpo = resposta.getBody();
        assertThat((String) corpo.get("ordem")).isEqualTo("ordem de chegada");

        List<Map<String, Object>> nos = (List<Map<String, Object>>) corpo.get("nos");
        long anterior = 0;
        for (Map<String, Object> no : nos) {
            long sequencia = ((Number) no.get("sequenciaEntrada")).longValue();
            assertThat(sequencia).isGreaterThan(anterior);
            anterior = sequencia;
        }
    }

    @Test
    @Order(6)
    @DisplayName("a alocacao devolve os passos reais e marca a politica como provisoria")
    void alocacaoProvisoria() {
        List<Map<String, Object>> fila = cliente.getForObject("/api/v1/requisicoes/fila", List.class);
        String codigo = (String) fila.get(0).get("codigo");

        List<Map<String, Object>> requisicoes =
                (List<Map<String, Object>>) cliente.getForObject("/api/v1/requisicoes?size=50", Map.class)
                        .get("content");
        Long id = requisicoes.stream()
                .filter(requisicao -> codigo.equals(requisicao.get("codigo")))
                .map(requisicao -> ((Number) requisicao.get("id")).longValue())
                .findFirst()
                .orElseThrow();

        ResponseEntity<Map> resposta = cliente.postForEntity(
                "/api/v1/requisicoes/" + id + "/alocacoes", null, Map.class);

        assertThat(resposta.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.CONFLICT);

        if (resposta.getStatusCode() == HttpStatus.CREATED) {
            Map<String, Object> corpo = resposta.getBody();
            assertThat((String) corpo.get("politicaAplicada")).isEqualTo("PROVISORIA_U1");
            assertThat((String) corpo.get("avisoAcademico")).contains("sem FEFO");
            assertThat((List<?>) corpo.get("passos")).isNotEmpty();
            assertThat((List<?>) corpo.get("bolsas")).isNotEmpty();
        } else {
            assertThat((String) resposta.getBody().get("detail")).contains("continua na fila");
        }
    }

    @Test
    @Order(7)
    @DisplayName("os indicadores sao calculados sobre os dados do sistema")
    void indicadores() {
        ResponseEntity<Map> estoque = cliente.getForEntity("/api/v1/indicadores/estoque", Map.class);

        assertThat(estoque.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) estoque.getBody().get("aviso")).contains("sinteticos");
        assertThat(((Number) estoque.getBody().get("totalDeBolsasDisponiveis")).longValue()).isPositive();

        Map<String, Object> distribuicao = (Map<String, Object>) estoque.getBody().get("distribuicaoPorTipo");
        assertThat(((Number) distribuicao.get("quantidade")).intValue()).isPositive();
    }

    @Test
    @Order(8)
    @DisplayName("erro de validacao responde no envelope padrao, com os campos")
    void envelopeDeErro() {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/hospitais",
                Map.of("nome", "", "cidade", "Recife", "uf", "PE"), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> corpo = resposta.getBody();
        assertThat((String) corpo.get("type")).contains("/erros/entrada-invalida");
        assertThat(corpo).containsEntry("status", 400);
        assertThat(corpo.get("instance")).isEqualTo("/api/v1/hospitais");
        assertThat((List<?>) corpo.get("campos")).isNotEmpty();
        assertThat(corpo.get("correlacao")).isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("recurso inexistente responde 404 no mesmo envelope")
    void naoEncontrado() {
        ResponseEntity<Map> resposta = cliente.getForEntity("/api/v1/bolsas/999999", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((String) resposta.getBody().get("type")).contains("/erros/recurso-nao-encontrado");
    }
}
