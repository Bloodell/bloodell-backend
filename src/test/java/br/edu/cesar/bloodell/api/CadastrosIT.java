package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate
class CadastrosIT extends TesteDeIntegracao {

    @Autowired
    private TestRestTemplate cliente;

    @Test
    @DisplayName("unidade sem historico e removida; com historico a API responde 409")
    void remocaoDeUnidades() {
        Long hospital = criarUnidade("/api/v1/hospitais", "Hospital Removivel");
        assertThat(excluir("/api/v1/hospitais/" + hospital)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cliente.getForEntity("/api/v1/hospitais/" + hospital, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        Long hemocentro = criarUnidade("/api/v1/hemocentros", "Hemocentro Com Doacao");
        registrarDoacao(hemocentro);
        assertThat(excluir("/api/v1/hemocentros/" + hemocentro)).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("DELETE em /hemocentros nao remove um hospital")
    void remocaoRespeitaOTipo() {
        Long hospital = criarUnidade("/api/v1/hospitais", "Hospital Protegido");

        assertThat(excluir("/api/v1/hemocentros/" + hospital)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cliente.getForEntity("/api/v1/hospitais/" + hospital, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("veiculo percorre cadastro, consulta, atualizacao e remocao")
    void crudDeVeiculo() {
        Long base = criarUnidade("/api/v1/hemocentros", "Hemocentro Frota");
        String placa = placa();

        ResponseEntity<Map> criado = cliente.postForEntity("/api/v1/veiculos", veiculo(placa, base, 20), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criado.getBody()).containsEntry("status", "DISPONIVEL");
        Long id = ((Number) criado.getBody().get("id")).longValue();

        assertThat(cliente.postForEntity("/api/v1/veiculos", veiculo(placa, base, 10), Map.class).getStatusCode())
                .as("placa repetida").isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> atualizado = cliente.exchange("/api/v1/veiculos/" + id, HttpMethod.PUT,
                new HttpEntity<>(veiculo(placa, base, 35)), Map.class);
        assertThat(atualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atualizado.getBody()).containsEntry("capacidadeBolsas", 35);

        assertThat(cliente.getForEntity("/api/v1/veiculos/" + id, Map.class).getBody())
                .containsEntry("capacidadeBolsas", 35);

        assertThat(excluir("/api/v1/veiculos/" + id)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cliente.getForEntity("/api/v1/veiculos/" + id, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("veiculo so pode ter um hemocentro como base")
    void baseDoVeiculoPrecisaSerHemocentro() {
        Long hospital = criarUnidade("/api/v1/hospitais", "Hospital Sem Frota");

        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/veiculos",
                veiculo(placa(), hospital, 20), Map.class);

        assertThat(resposta.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    @DisplayName("doacao registrada aparece na listagem e no detalhe, com as bolsas geradas")
    void consultaDeDoacoes() {
        Long hemocentro = criarUnidade("/api/v1/hemocentros", "Hemocentro Consulta");
        Long doacao = registrarDoacao(hemocentro);

        ResponseEntity<List> lista = cliente.getForEntity("/api/v1/doacoes", List.class);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).anySatisfy(item ->
                assertThat(((Number) ((Map<?, ?>) item).get("id")).longValue()).isEqualTo(doacao));

        ResponseEntity<Map> detalhe = cliente.getForEntity("/api/v1/doacoes/" + doacao, Map.class);
        assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detalhe.getBody()).containsEntry("tipoSanguineo", "O-");
        assertThat((List<?>) detalhe.getBody().get("bolsasGeradas")).hasSize(2);

        assertThat(cliente.getForEntity("/api/v1/doacoes/999999", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long criarUnidade(String recurso, String nome) {
        ResponseEntity<Map> resposta = cliente.postForEntity(recurso, Map.of(
                "nome", nome + " " + UUID.randomUUID().toString().substring(0, 8),
                "cidade", "Recife", "uf", "PE", "latitude", -8.05, "longitude", -34.88), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resposta.getBody().get("id")).longValue();
    }

    private Long registrarDoacao(Long hemocentro) {
        ResponseEntity<Map> resposta = cliente.postForEntity("/api/v1/doacoes", Map.of(
                "hemocentroId", hemocentro, "tipoSanguineo", "O-", "volumeMl", 450,
                "componentes", List.of("CONCENTRADO_HEMACIAS", "PLASMA")), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resposta.getBody().get("id")).longValue();
    }

    private Map<String, Object> veiculo(String placa, Long base, int capacidade) {
        return Map.of("placa", placa, "hemocentroBaseId", base, "capacidadeBolsas", capacidade,
                "latitude", -8.05, "longitude", -34.88);
    }

    private String placa() {
        return "T" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
    }

    private HttpStatus excluir(String caminho) {
        return HttpStatus.valueOf(cliente.exchange(caminho, HttpMethod.DELETE, null, Void.class)
                .getStatusCode().value());
    }
}
