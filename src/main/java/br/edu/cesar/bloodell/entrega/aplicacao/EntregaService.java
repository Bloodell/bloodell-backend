package br.edu.cesar.bloodell.entrega.aplicacao;

import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaEntrada;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaResposta;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.PlanejamentoEntregaEntrada;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaService {

    private final EntregaRepository entregas;
    private final RequisicaoRepository requisicoes;
    private final VeiculoRepository veiculos;
    private final UnidadeRepository unidades;

    public EntregaService(EntregaRepository entregas, RequisicaoRepository requisicoes,
                          VeiculoRepository veiculos, UnidadeRepository unidades) {
        this.entregas = entregas;
        this.requisicoes = requisicoes;
        this.veiculos = veiculos;
        this.unidades = unidades;
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

    public static EntregaResposta resposta(Entrega entrega) {
        return new EntregaResposta(entrega.getId(), entrega.getRequisicao().getId(),
                entrega.getRequisicao().getCodigo(), entrega.getVeiculo().getId(),
                entrega.getVeiculo().getPlaca(), entrega.getOrigem().getId(), entrega.getOrigem().getNome(),
                entrega.getStatus().name(), entrega.getRota(), entrega.getDistanciaKm(),
                entrega.getTempoEstimadoMin(), entrega.getIniciadaEm(), entrega.getConcluidaEm());
    }
}
