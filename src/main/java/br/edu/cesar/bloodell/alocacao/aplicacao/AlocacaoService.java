package br.edu.cesar.bloodell.alocacao.aplicacao;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;
import br.edu.cesar.bloodell.alocacao.apresentacao.AlocacaoDtos.BolsaAlocadaResposta;
import br.edu.cesar.bloodell.alocacao.apresentacao.AlocacaoDtos.ResultadoAlocacaoResposta;
import br.edu.cesar.bloodell.alocacao.dominio.DiarioDaDecisao;
import br.edu.cesar.bloodell.alocacao.dominio.PoliticaDeAlocacao;
import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.estoque.aplicacao.SincronizadorDeEstoque;
import br.edu.cesar.bloodell.rastreabilidade.aplicacao.RegistradorDeEventos;
import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import br.edu.cesar.bloodell.requisicao.dominio.ItemRequisicao;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlocacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(AlocacaoService.class);

    private final RequisicaoRepository requisicoes;
    private final RequisicaoService requisicaoService;
    private final BolsaRepository bolsas;
    private final UnidadeRepository unidades;
    private final PoliticaDeAlocacao politica;
    private final SincronizadorDeEstoque sincronizador;
    private final RegistradorDeEventos registrador;

    public AlocacaoService(RequisicaoRepository requisicoes,
                           RequisicaoService requisicaoService,
                           BolsaRepository bolsas,
                           UnidadeRepository unidades,
                           PoliticaDeAlocacao politica,
                           SincronizadorDeEstoque sincronizador,
                           RegistradorDeEventos registrador) {
        this.requisicoes = requisicoes;
        this.requisicaoService = requisicaoService;
        this.bolsas = bolsas;
        this.unidades = unidades;
        this.politica = politica;
        this.sincronizador = sincronizador;
        this.registrador = registrador;
    }

    @Transactional
    public ResultadoAlocacaoResposta processar(Long requisicaoId) {
        Requisicao requisicao = requisicaoService.buscarEntidade(requisicaoId);
        DiarioDaDecisao diario = new DiarioDaDecisao();

        if (requisicao.getStatus() != StatusRequisicao.NA_FILA) {
            throw new RegraDeNegocioException("A requisicao " + requisicao.getCodigo()
                    + " esta em " + requisicao.getStatus() + " e nao pode ser processada agora.");
        }

        var fila = requisicaoService.montarFilaAPartirDoBanco();
        var proxima = fila.espiar();
        diario.anotar("fila",
                "Fila da AED-U1 montada com " + fila.tamanho() + " requisicao(oes) aguardando. "
                        + "Proxima por prioridade e chegada: "
                        + (proxima == null ? "nenhuma" : proxima.getCodigo()),
                requisicao.getCodigo().equals(proxima == null ? null : proxima.getCodigo())
                        ? "esta requisicao e a proxima da fila"
                        : "processamento fora da ordem da fila (permitido, mas registrado)");

        requisicao.mudarPara(StatusRequisicao.EM_PROCESSAMENTO);

        var hemocentros = unidades.listarHemocentros();
        if (hemocentros.isEmpty()) {
            throw new RegraDeNegocioException("Nao ha hemocentro cadastrado para atender a requisicao.");
        }
        Long hemocentroId = hemocentros.get(0).getId();
        diario.anotar("origem",
                "Hemocentro de origem: " + hemocentros.get(0).getNome(),
                "escolha simples da U1 -- selecao por distancia depende de roteirizacao (U2)");

        ListaEstoque estoque = montarListaDeEstoque(hemocentroId);

        LocalDate hoje = LocalDate.now();
        List<BolsaAlocadaResposta> alocadas = new ArrayList<>();
        int solicitado = 0;

        for (ItemRequisicao item : requisicao.getItens()) {
            solicitado += item.getQuantidade();
            String tipoPedido = item.getTipoSolicitado().formatado();
            String componente = item.getComponente().name();

            while (!item.totalmenteAtendido()) {
                BolsaAed candidata = politica.escolher(estoque, tipoPedido, componente, diario);
                if (candidata == null) {
                    break;
                }

                Bolsa bolsa = bolsas.findByCodigo(candidata.getCodigo())
                        .orElseThrow(() -> new ConflitoException(
                                "A bolsa " + candidata.getCodigo() + " saiu do estoque durante o processamento."));

                if (!bolsa.podeSerAlocada(hoje)) {
                    diario.anotar("validade",
                            "Bolsa " + bolsa.getCodigo() + " descartada da selecao (status "
                                    + bolsa.getStatus() + ", validade " + bolsa.getDataValidade() + ")",
                            "bolsa ignorada");
                    estoque.removerPorCodigo(bolsa.getCodigo());
                    continue;
                }

                EstadoBolsa anterior = bolsa.getStatus();
                bolsa.reservarPara(hoje);
                registrador.registrar(bolsa, anterior, bolsa.getStatus(), requisicao.getId(),
                        "Reservada para a requisicao " + requisicao.getCodigo());

                bolsa.confirmarAlocacao();
                registrador.registrar(bolsa, EstadoBolsa.RESERVADA, bolsa.getStatus(), requisicao.getId(),
                        "Alocada pela politica " + politica.nome());

                sincronizador.registrarSaida(bolsa);
                item.registrarAlocacao(1);
                estoque.removerPorCodigo(bolsa.getCodigo());

                alocadas.add(new BolsaAlocadaResposta(bolsa.getId(), bolsa.getCodigo(),
                        bolsa.getTipoSanguineo().formatado(), bolsa.getComponente().name(),
                        bolsa.getDataValidade().toString(), bolsa.getSequenciaEntrada()));
            }
        }

        if (alocadas.isEmpty()) {
            requisicao.mudarPara(StatusRequisicao.REJEITADA);
            requisicao.mudarPara(StatusRequisicao.NA_FILA);
            requisicoes.save(requisicao);
            LOG.info("Requisicao {} sem bolsa elegivel; devolvida a fila.", requisicao.getCodigo());
            throw new ConflitoException("Nao ha bolsa disponivel compativel com a requisicao "
                    + requisicao.getCodigo() + " neste momento. O pedido continua na fila.");
        }

        requisicao.mudarPara(StatusRequisicao.ALOCADA);
        requisicoes.save(requisicao);

        int quantidadeAlocada = alocadas.size();
        LOG.info("Requisicao {} alocada com {} bolsa(s) pela politica {}.",
                requisicao.getCodigo(), quantidadeAlocada, politica.nome());

        return new ResultadoAlocacaoResposta(
                requisicao.getId(),
                requisicao.getCodigo(),
                requisicao.getStatus().name(),
                politica.nome(),
                politica.aviso(),
                solicitado,
                quantidadeAlocada,
                alocadas,
                diario.getPassos());
    }

    private ListaEstoque montarListaDeEstoque(Long unidadeId) {
        ListaEstoque lista = new ListaEstoque();
        for (Bolsa bolsa : bolsas.listarPorUnidadeNaOrdemDeChegada(unidadeId)) {
            lista.inserir(new BolsaAed(
                    bolsa.getCodigo(),
                    bolsa.getTipoSanguineo().formatado(),
                    bolsa.getComponente().name(),
                    bolsa.getDataValidade().toString(),
                    bolsa.getSequenciaEntrada(),
                    bolsa.getStatus().name()));
        }
        return lista;
    }
}
