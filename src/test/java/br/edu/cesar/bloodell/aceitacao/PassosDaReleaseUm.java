package br.edu.cesar.bloodell.aceitacao;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Passos das três histórias da Release 1. Tudo passa pela API HTTP real: os passos
 * não chamam service nem repository, e as conferências do estado persistido são
 * feitas por uma nova consulta à API.
 */
public class PassosDaReleaseUm {

    @Autowired
    private TestRestTemplate cliente;

    // Estado do cenário. O Cucumber cria uma instância desta classe por cenário.
    private ResponseEntity<Object> ultimaResposta;
    private Long hemocentroId;
    private Long hospitalId;
    private Map<String, Object> doacao;
    private Long requisicaoId;
    private final Map<String, Long> requisicoesPorApelido = new LinkedHashMap<>();
    private final Map<String, Long> bolsasPorApelido = new HashMap<>();

    // ---------------------------------------------------------------- comuns

    @Dado("um hemocentro cadastrado")
    public void umHemocentroCadastrado() {
        hemocentroId = criarUnidade("/api/v1/hemocentros", "Hemocentro Aceitacao");
    }

    @Dado("um hospital cadastrado")
    public void umHospitalCadastrado() {
        hospitalId = criarUnidade("/api/v1/hospitais", "Hospital Aceitacao");
    }

    @Entao("a API responde {int}")
    public void aApiResponde(int status) {
        assertThat(ultimaResposta.getStatusCode().value())
                .as("corpo da resposta: %s", ultimaResposta.getBody())
                .isEqualTo(status);
    }

    @E("a resposta aponta erro no campo {string}")
    public void aRespostaApontaErroNoCampo(String campo) {
        List<Map<String, Object>> campos = lista(corpo().get("campos"));
        assertThat(campos).extracting(item -> item.get("campo")).contains(campo);
    }

    @E("a resposta explica que {string}")
    public void aRespostaExplicaQue(String trecho) {
        assertThat((String) corpo().get("detail")).contains(trecho);
    }

    // ------------------------------------------------------------- US-R1-01

    @Quando("registro no {word} uma doação {string} de {int} ml com os componentes:")
    public void registroUmaDoacao(String unidade, String tipo, int volume, List<String> componentes) {
        Long unidadeId = "hospital".equals(unidade) ? hospitalId : hemocentroId;
        ultimaResposta = enviar(HttpMethod.POST, "/api/v1/doacoes", Map.of(
                "hemocentroId", unidadeId, "tipoSanguineo", tipo, "volumeMl", volume,
                "componentes", componentes));
        if (ultimaResposta.getStatusCode().is2xxSuccessful()) {
            doacao = corpo();
        }
    }

    @E("a doação gera {int} bolsas com status {string}")
    public void aDoacaoGeraBolsas(int quantidade, String status) {
        List<Map<String, Object>> bolsas = lista(doacao.get("bolsasGeradas"));
        assertThat(bolsas).hasSize(quantidade).allSatisfy(bolsa -> assertThat(bolsa).containsEntry("status", status));
    }

    @E("a bolsa de {string} vence {int} dias após a coleta")
    public void aBolsaVenceAposAColeta(String componente, int dias) {
        Long id = lista(doacao.get("bolsasGeradas")).stream()
                .filter(bolsa -> componente.equals(bolsa.get("componente")))
                .map(bolsa -> numero(bolsa.get("id")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> bolsa = mapa(consultar("/api/v1/bolsas/" + id).getBody());

        LocalDate coleta = LocalDate.parse((String) bolsa.get("dataColeta"));
        LocalDate validade = LocalDate.parse((String) bolsa.get("dataValidade"));
        assertThat(ChronoUnit.DAYS.between(coleta, validade)).isEqualTo(dias);
    }

    @E("ao consultar a doação novamente ela tem {int} bolsas")
    public void aoConsultarADoacaoNovamente(int quantidade) {
        ResponseEntity<Object> resposta = consultar("/api/v1/doacoes/" + numero(doacao.get("id")));
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lista(mapa(resposta.getBody()).get("bolsasGeradas"))).hasSize(quantidade);
    }

    @E("o estoque do hemocentro tem {int} bolsa {string} de {string}")
    public void oEstoqueDoHemocentroTem(int quantidade, String tipo, String componente) {
        List<Map<String, Object>> itens = lista(consultar(
                "/api/v1/estoques?unidadeId=" + hemocentroId + "&componente=" + componente).getBody());
        assertThat(itens)
                .filteredOn(item -> tipo.equals(item.get("tipoSanguineo")))
                .singleElement()
                .satisfies(item -> assertThat(numero(item.get("quantidade"))).isEqualTo((long) quantidade));
    }

    @E("nenhuma doação foi registrada")
    public void nenhumaDoacaoFoiRegistrada() {
        assertThat(lista(consultar("/api/v1/doacoes").getBody())).isEmpty();
    }

    // ------------------------------------------------------------- US-R1-02

    @Quando("o hospital cria as requisições, nesta ordem:")
    public void oHospitalCriaAsRequisicoes(List<Map<String, String>> linhas) {
        for (Map<String, String> linha : linhas) {
            criarRequisicao(linha.get("prioridade"), List.of(item(1, "CONCENTRADO_HEMACIAS", "A+")));
            assertThat(ultimaResposta.getStatusCode().value()).isEqualTo(201);
            requisicoesPorApelido.put(linha.get("requisicao"), requisicaoId);
        }
    }

    @Quando("o hospital cria uma requisição {string} sem itens")
    public void oHospitalCriaUmaRequisicaoSemItens(String prioridade) {
        criarRequisicao(prioridade, List.of());
    }

    @Quando("o hospital cria uma requisição {string} de {int} {string} {string}")
    public void oHospitalCriaUmaRequisicao(String prioridade, int quantidade, String componente, String tipo) {
        criarRequisicao(prioridade, List.of(item(quantidade, componente, tipo)));
    }

    @Entao("a fila de atendimento é {string}")
    public void aFilaDeAtendimentoE(String ordemEsperada) {
        Map<String, String> apelidoPorCodigo = new HashMap<>();
        requisicoesPorApelido.forEach((apelido, id) ->
                apelidoPorCodigo.put((String) mapa(consultar("/api/v1/requisicoes/" + id).getBody()).get("codigo"),
                        apelido));

        List<String> ordem = lista(consultar("/api/v1/requisicoes/fila").getBody()).stream()
                .map(item -> apelidoPorCodigo.get((String) item.get("codigo")))
                .toList();
        assertThat(ordem).containsExactlyElementsOf(Arrays.asList(ordemEsperada.split(",\\s*")));
    }

    @Entao("a fila de atendimento está vazia")
    public void aFilaDeAtendimentoEstaVazia() {
        ResponseEntity<Object> resposta = consultar("/api/v1/requisicoes/fila");
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lista(resposta.getBody())).isEmpty();
    }

    @E("cada requisição criada pode ser consultada com status {string}")
    public void cadaRequisicaoPodeSerConsultada(String status) {
        requisicoesPorApelido.values().forEach(id ->
                assertThat(mapa(consultar("/api/v1/requisicoes/" + id).getBody())).containsEntry("status", status));
    }

    // ------------------------------------------------------------- US-R1-03

    @Dado("as doações registradas no hemocentro, nesta ordem:")
    public void asDoacoesRegistradas(List<Map<String, String>> linhas) {
        for (Map<String, String> linha : linhas) {
            OffsetDateTime coleta = OffsetDateTime.now().minusDays(Long.parseLong(linha.get("coletada ha dias")));
            ResponseEntity<Object> resposta = enviar(HttpMethod.POST, "/api/v1/doacoes", Map.of(
                    "hemocentroId", hemocentroId, "tipoSanguineo", linha.get("tipo"), "volumeMl", 450,
                    "dataHora", coleta.toString(), "componentes", List.of(linha.get("componente"))));
            assertThat(resposta.getStatusCode().value()).as("doacao %s", linha).isEqualTo(201);

            Map<String, Object> bolsa = lista(mapa(resposta.getBody()).get("bolsasGeradas")).getFirst();
            bolsasPorApelido.put(linha.get("bolsa"), numero(bolsa.get("id")));
        }
    }

    @Quando("processo a alocação da requisição")
    public void processoAAlocacao() {
        ultimaResposta = enviar(HttpMethod.POST, "/api/v1/requisicoes/" + requisicaoId + "/alocacoes", null);
    }

    @E("a política aplicada é {string}")
    public void aPoliticaAplicadaE(String politica) {
        assertThat(corpo()).containsEntry("politicaAplicada", politica);
    }

    @E("a bolsa alocada é {string}")
    public void aBolsaAlocadaE(String apelido) {
        assertThat(lista(corpo().get("bolsas")))
                .extracting(bolsa -> numero(bolsa.get("bolsaId")))
                .containsExactly(bolsasPorApelido.get(apelido));
    }

    @E("ao consultar novamente a requisição está com status {string}")
    public void aoConsultarNovamenteARequisicao(String status) {
        assertThat(mapa(consultar("/api/v1/requisicoes/" + requisicaoId).getBody())).containsEntry("status", status);
    }

    @E("a bolsa {string} está com status {string}")
    public void aBolsaEstaComStatus(String apelido, String status) {
        assertThat(mapa(consultar("/api/v1/bolsas/" + bolsasPorApelido.get(apelido)).getBody()))
                .containsEntry("status", status);
    }

    // ------------------------------------------------------------- apoio

    private Long criarUnidade(String recurso, String nome) {
        ResponseEntity<Object> resposta = enviar(HttpMethod.POST, recurso, Map.of(
                "nome", nome, "cidade", "Recife", "uf", "PE", "latitude", -8.05, "longitude", -34.88));
        assertThat(resposta.getStatusCode().value()).as("cadastro em %s", recurso).isEqualTo(201);
        return numero(mapa(resposta.getBody()).get("id"));
    }

    private void criarRequisicao(String prioridade, List<Map<String, Object>> itens) {
        ultimaResposta = enviar(HttpMethod.POST, "/api/v1/requisicoes", Map.of(
                "hospitalId", hospitalId, "prioridade", prioridade, "itens", itens));
        if (ultimaResposta.getStatusCode().is2xxSuccessful()) {
            requisicaoId = numero(corpo().get("id"));
        }
    }

    private Map<String, Object> item(int quantidade, String componente, String tipo) {
        return Map.of("componente", componente, "tipoSanguineo", tipo, "quantidade", quantidade);
    }

    private ResponseEntity<Object> enviar(HttpMethod metodo, String url, Object corpo) {
        return cliente.exchange(url, metodo, new HttpEntity<>(corpo), Object.class);
    }

    private ResponseEntity<Object> consultar(String url) {
        return enviar(HttpMethod.GET, url, null);
    }

    private Map<String, Object> corpo() {
        return mapa(ultimaResposta.getBody());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return (Map<String, Object>) valor;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object valor) {
        return (List<Map<String, Object>>) valor;
    }

    private static Long numero(Object valor) {
        return ((Number) valor).longValue();
    }
}
