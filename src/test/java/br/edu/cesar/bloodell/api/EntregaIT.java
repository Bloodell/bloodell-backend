package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.entrega.infraestrutura.EntregaRepository;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Hospital;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate
class EntregaIT extends TesteDeIntegracao {

    @Autowired private TestRestTemplate cliente;
    @Autowired private UnidadeRepository unidades;
    @Autowired private VeiculoRepository veiculos;
    @Autowired private RequisicaoRepository requisicoes;
    @Autowired private EntregaRepository entregas;

    @Test
    @DisplayName("a API planeja, despacha, transporta e conclui uma entrega persistida")
    void fluxoCompletoDeEntrega() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Unidade origem = unidades.save(new Hemocentro("HEM " + sufixo, endereco(), posicao()));
        Unidade hospital = unidades.save(new Hospital("HOS " + sufixo, endereco(), posicao()));
        Veiculo veiculo = veiculos.save(new Veiculo("T" + sufixo.substring(0, 7), origem, 20, posicao()));
        Requisicao requisicao = new Requisicao("REQ-" + sufixo, hospital, Prioridade.URGENTE);
        requisicao.entrarNaFila();
        requisicao.mudarPara(StatusRequisicao.EM_PROCESSAMENTO);
        requisicao.mudarPara(StatusRequisicao.ALOCADA);
        requisicao = requisicoes.save(requisicao);

        ResponseEntity<Map> planejada = cliente.postForEntity("/api/v1/entregas",
                Map.of("requisicaoId", requisicao.getId(), "veiculoId", veiculo.getId(), "origemId", origem.getId(),
                        "rota", "{\"paradas\":[]}", "distanciaKm", 12.5, "tempoEstimadoMin", 25), Map.class);

        assertThat(planejada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long entregaId = ((Number) planejada.getBody().get("id")).longValue();
        assertThat(cliente.postForEntity("/api/v1/entregas/" + entregaId + "/despacho", null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cliente.postForEntity("/api/v1/entregas/" + entregaId + "/inicio-transporte", null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> concluida = cliente.postForEntity("/api/v1/entregas/" + entregaId + "/conclusao", null, Map.class);

        assertThat(concluida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(concluida.getBody()).containsEntry("status", "ENTREGUE");
        assertThat(entregas.findById(entregaId).orElseThrow().getConcluidaEm()).isNotNull();
        assertThat(requisicoes.findById(requisicao.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusRequisicao.ATENDIDA);
    }

    private Endereco endereco() {
        return new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000");
    }

    private CoordenadaGeo posicao() {
        return new CoordenadaGeo(-8.0578, -34.8829);
    }
}
