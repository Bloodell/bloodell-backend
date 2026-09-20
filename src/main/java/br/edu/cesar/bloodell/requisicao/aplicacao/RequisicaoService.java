package br.edu.cesar.bloodell.requisicao.aplicacao;

import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;
import br.edu.cesar.bloodell.aed.u1.FilaRequisicoes;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemDaFilaResposta;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemRequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemRequisicaoResposta;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoResposta;
import br.edu.cesar.bloodell.requisicao.dominio.ItemRequisicao;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequisicaoService {

    private static final Logger LOG = LoggerFactory.getLogger(RequisicaoService.class);

    private final RequisicaoRepository requisicoes;
    private final UnidadeService unidadeService;

    public RequisicaoService(RequisicaoRepository requisicoes, UnidadeService unidadeService) {
        this.requisicoes = requisicoes;
        this.unidadeService = unidadeService;
    }

    @Transactional
    public RequisicaoResposta criar(RequisicaoEntrada entrada) {
        Unidade hospital = unidadeService.buscarEntidade(entrada.hospitalId());
        if (!"HOSPITAL".equals(hospital.tipo())) {
            throw new RegraDeNegocioException("Requisicao so pode ser criada por um hospital. A unidade "
                    + hospital.getNome() + " e um " + hospital.tipo().toLowerCase() + ".");
        }

        Requisicao requisicao = new Requisicao(gerarCodigo(), hospital, entrada.prioridade());
        for (ItemRequisicaoEntrada item : entrada.itens()) {
            requisicao.adicionarItem(new ItemRequisicao(requisicao, item.componente(),
                    TipoSanguineo.doTextoCompacto(item.tipoSanguineo()), item.quantidade()));
        }
        requisicao.entrarNaFila();
        Requisicao salva = requisicoes.save(requisicao);

        LOG.info("Requisicao {} criada pelo hospital {} com prioridade {}.",
                salva.getCodigo(), hospital.getNome(), salva.getPrioridade());
        return paraResposta(salva);
    }

    @Transactional(readOnly = true)
    public Page<RequisicaoResposta> listar(StatusRequisicao status, Prioridade prioridade,
                                           Long hospitalId, Pageable paginacao) {
        return requisicoes.buscarComFiltros(status, prioridade, hospitalId, paginacao)
                .map(RequisicaoService::paraResposta);
    }

    @Transactional(readOnly = true)
    public RequisicaoResposta detalhar(Long id) {
        return paraResposta(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Requisicao buscarEntidade(Long id) {
        return requisicoes.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Requisicao", id));
    }

    @Transactional(readOnly = true)
    public List<ItemDaFilaResposta> estadoDaFila() {
        FilaRequisicoes fila = montarFilaAPartirDoBanco();
        RequisicaoAed[] ordenadas = fila.paraVetor();
        List<ItemDaFilaResposta> resposta = new ArrayList<>(ordenadas.length);
        for (int indice = 0; indice < ordenadas.length; indice++) {
            RequisicaoAed item = ordenadas[indice];
            resposta.add(new ItemDaFilaResposta(indice + 1, item.getCodigo(), item.getHospital(),
                    item.getPrioridade(), item.getSequenciaChegada()));
        }
        return resposta;
    }

    @Transactional(readOnly = true)
    public FilaRequisicoes montarFilaAPartirDoBanco() {
        FilaRequisicoes fila = new FilaRequisicoes();
        List<Requisicao> aguardando = requisicoes.listarNaOrdemDeChegada(StatusRequisicao.NA_FILA);
        for (Requisicao requisicao : aguardando) {
            fila.enfileirar(new RequisicaoAed(
                    requisicao.getCodigo(),
                    requisicao.getHospital().getNome(),
                    requisicao.getPrioridade().name(),
                    requisicao.getId()));
        }
        return fila;
    }

    private String gerarCodigo() {
        long proximo = requisicoes.count() + 1;
        String codigo = String.format("REQ-%06d", proximo);
        while (requisicoes.findByCodigo(codigo).isPresent()) {
            proximo++;
            codigo = String.format("REQ-%06d", proximo);
        }
        return codigo;
    }

    public static RequisicaoResposta paraResposta(Requisicao requisicao) {
        List<ItemRequisicaoResposta> itens = new ArrayList<>();
        for (ItemRequisicao item : requisicao.getItens()) {
            itens.add(new ItemRequisicaoResposta(
                    item.getId(),
                    item.getComponente().name(),
                    item.getTipoSolicitado().formatado(),
                    item.getQuantidade(),
                    item.getQuantidadeAlocada()));
        }
        return new RequisicaoResposta(
                requisicao.getId(),
                requisicao.getCodigo(),
                requisicao.getHospital().getId(),
                requisicao.getHospital().getNome(),
                requisicao.getPrioridade().name(),
                requisicao.getStatus().name(),
                requisicao.getCriadaEm(),
                requisicao.getAtendidaEm(),
                itens);
    }
}
