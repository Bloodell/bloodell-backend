package br.edu.cesar.bloodell.entrega.aplicacao;

import br.edu.cesar.bloodell.aed.u2.Roteirizador;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaEntrada;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaResposta;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.ParadaRota;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.PlanejamentoEntregaEntrada;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.RotaPlanejada;
import br.edu.cesar.bloodell.entrega.dominio.Entrega;
import br.edu.cesar.bloodell.entrega.dominio.StatusEntrega;
import br.edu.cesar.bloodell.entrega.infraestrutura.EntregaRepository;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaService {

    private final EntregaRepository entregas;
    private final RequisicaoRepository requisicoes;
    private final VeiculoRepository veiculos;
    private final UnidadeRepository unidades;
    private final Roteirizador roteirizador;
    private final ObjectMapper objectMapper;

    public EntregaService(EntregaRepository entregas, RequisicaoRepository requisicoes,
                          VeiculoRepository veiculos, UnidadeRepository unidades,
                          Roteirizador roteirizador, ObjectMapper objectMapper) {
        this.entregas = entregas;
        this.requisicoes = requisicoes;
        this.veiculos = veiculos;
        this.unidades = unidades;
        this.roteirizador = roteirizador;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EntregaResposta criar(EntregaEntrada entrada) {
        Requisicao requisicao = requisicao(entrada.requisicaoId());
        if (requisicao.getStatus() != StatusRequisicao.ALOCADA) {
            throw new RegraDeNegocioException("A entrega so pode ser planejada para uma requisicao alocada.");
        }
        if (entregas.existsByRequisicaoIdAndStatusIn(requisicao.getId(),
                List.of(StatusEntrega.PLANEJADA, StatusEntrega.DESPACHADA, StatusEntrega.EM_TRANSPORTE))) {
            throw new ConflitoException("A requisicao " + requisicao.getCodigo() + " ja possui uma entrega ativa.");
        }
        Veiculo veiculo = veiculo(entrada.veiculoId());
        if (veiculo.getStatus() != StatusVeiculo.DISPONIVEL) {
            throw new RegraDeNegocioException("O veiculo " + veiculo.getPlaca() + " nao esta disponivel.");
        }
        Unidade origem = unidade(entrada.origemId());
        if (!"HEMOCENTRO".equals(origem.tipo())) {
            throw new RegraDeNegocioException("A origem de uma entrega precisa ser um hemocentro.");
        }
        return resposta(entregas.save(new Entrega(requisicao, veiculo, origem, entrada.rota(),
                entrada.distanciaKm(), entrada.tempoEstimadoMin())));
    }

    @Transactional(readOnly = true)
    public List<EntregaResposta> listar(StatusEntrega status) {
        List<Entrega> encontradas = status == null ? entregas.findAll() : entregas.findByStatusOrderByIdDesc(status);
        return encontradas.stream().map(EntregaService::resposta).toList();
    }

    @Transactional(readOnly = true)
    public EntregaResposta detalhar(Long id) {
        return resposta(buscar(id));
    }

    @Transactional
    public EntregaResposta atualizarPlanejamento(Long id, PlanejamentoEntregaEntrada entrada) {
        Entrega entrega = buscar(id);
        entrega.atualizarPlanejamento(entrada.rota(), entrada.distanciaKm(), entrada.tempoEstimadoMin());
        return resposta(entrega);
    }

    @Transactional
    public EntregaResposta roteirizar(Long id) {
        Entrega entrega = buscar(id);
        if (entrega.getStatus() != StatusEntrega.PLANEJADA) {
            throw new RegraDeNegocioException("A rota so pode ser calculada enquanto a entrega estiver planejada.");
        }

        Unidade origem = entrega.getOrigem();
        Unidade destino = entrega.getRequisicao().getHospital();
        CoordenadaGeo coordenadaOrigem = origem.getLocalizacao();
        CoordenadaGeo coordenadaDestino = destino.getLocalizacao();
        int[] ordem = roteirizador.calcularOrdemDeVisita(
                coordenadaOrigem.getLatitude(), coordenadaOrigem.getLongitude(),
                new double[]{coordenadaDestino.getLatitude()},
                new double[]{coordenadaDestino.getLongitude()});
        if (ordem.length != 1 || ordem[0] != 0) {
            throw new IllegalStateException("Roteirizador deve visitar o hospital da entrega exatamente uma vez.");
        }

        double distanciaKm = coordenadaOrigem.distanciaKm(coordenadaDestino);
        List<ParadaRota> paradas = List.of(parada(0, origem), parada(1, destino));
        entrega.atualizarPlanejamento(serializarRota(paradas),
                BigDecimal.valueOf(distanciaKm).setScale(3, RoundingMode.HALF_UP),
                entrega.getTempoEstimadoMin());
        return resposta(entrega);
    }

    @Transactional
    public EntregaResposta despachar(Long id) {
        Entrega entrega = buscar(id);
        entrega.mudarPara(StatusEntrega.DESPACHADA);
        entrega.getVeiculo().mudarStatus(StatusVeiculo.EM_ROTA);
        entrega.getRequisicao().mudarPara(StatusRequisicao.EM_ROTA);
        return resposta(entrega);
    }

    @Transactional
    public EntregaResposta iniciarTransporte(Long id) {
        Entrega entrega = buscar(id);
        entrega.mudarPara(StatusEntrega.EM_TRANSPORTE);
        return resposta(entrega);
    }

    @Transactional
    public EntregaResposta concluir(Long id) {
        Entrega entrega = buscar(id);
        entrega.mudarPara(StatusEntrega.ENTREGUE);
        entrega.getVeiculo().mudarStatus(StatusVeiculo.DISPONIVEL);
        entrega.getRequisicao().mudarPara(StatusRequisicao.ATENDIDA);
        return resposta(entrega);
    }

    @Transactional
    public void cancelar(Long id) {
        Entrega entrega = buscar(id);
        entrega.mudarPara(StatusEntrega.CANCELADA);
        entrega.getVeiculo().mudarStatus(StatusVeiculo.DISPONIVEL);
        StatusRequisicao status = entrega.getRequisicao().getStatus();
        if (status == StatusRequisicao.ALOCADA || status == StatusRequisicao.EM_ROTA) {
            entrega.getRequisicao().mudarPara(StatusRequisicao.CANCELADA);
        }
    }

    private Entrega buscar(Long id) {
        return entregas.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Entrega", id));
    }

    private Requisicao requisicao(Long id) {
        return requisicoes.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Requisicao", id));
    }

    private Veiculo veiculo(Long id) {
        return veiculos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Veiculo", id));
    }

    private Unidade unidade(Long id) {
        return unidades.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }

    private static ParadaRota parada(int ordem, Unidade unidade) {
        CoordenadaGeo localizacao = unidade.getLocalizacao();
        return new ParadaRota(ordem, unidade.tipo(), unidade.getId(), unidade.getNome(),
                localizacao.getLatitude(), localizacao.getLongitude());
    }

    private String serializarRota(List<ParadaRota> paradas) {
        try {
            return objectMapper.writeValueAsString(new RotaPlanejada(paradas));
        } catch (JsonProcessingException excecao) {
            throw new IllegalStateException("Nao foi possivel serializar a rota calculada.", excecao);
        }
    }

    public static EntregaResposta resposta(Entrega entrega) {
        return new EntregaResposta(entrega.getId(), entrega.getRequisicao().getId(),
                entrega.getRequisicao().getCodigo(), entrega.getVeiculo().getId(),
                entrega.getVeiculo().getPlaca(), entrega.getOrigem().getId(), entrega.getOrigem().getNome(),
                entrega.getStatus().name(), entrega.getRota(), entrega.getDistanciaKm(),
                entrega.getTempoEstimadoMin(), entrega.getIniciadaEm(), entrega.getConcluidaEm());
    }
}
