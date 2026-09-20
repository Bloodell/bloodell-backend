package br.edu.cesar.bloodell.algoritmos.aplicacao;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.contratos.EventoAed;
import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;
import br.edu.cesar.bloodell.aed.u1.FilaRequisicoes;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;
import br.edu.cesar.bloodell.aed.u1.PilhaHistorico;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.NoDaFila;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.NoDaLista;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.NoDaPilha;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SituacaoDoModuloAed;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaFila;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaLista;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaPilha;
import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import br.edu.cesar.bloodell.rastreabilidade.infraestrutura.EventoRastreabilidadeRepository;
import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlgoritmoService {

    private static final int LIMITE_DE_EVENTOS_NA_PILHA = 50;

    private final BolsaRepository bolsas;
    private final EventoRastreabilidadeRepository eventos;
    private final RequisicaoService requisicaoService;

    public AlgoritmoService(BolsaRepository bolsas,
                            EventoRastreabilidadeRepository eventos,
                            RequisicaoService requisicaoService) {
        this.bolsas = bolsas;
        this.eventos = eventos;
        this.requisicaoService = requisicaoService;
    }

    @Transactional(readOnly = true)
    public SnapshotDaLista estoque(Long unidadeId) {
        ListaEstoque lista = new ListaEstoque();
        List<Bolsa> doBanco = unidadeId == null
                ? bolsas.listarTodasNaOrdemDeChegada()
                : bolsas.listarPorUnidadeNaOrdemDeChegada(unidadeId);
        for (Bolsa bolsa : doBanco) {
            lista.inserir(new BolsaAed(bolsa.getCodigo(), bolsa.getTipoSanguineo().formatado(),
                    bolsa.getComponente().name(), bolsa.getDataValidade().toString(),
                    bolsa.getSequenciaEntrada(), bolsa.getStatus().name()));
        }

        BolsaAed[] nos = lista.paraVetor();
        List<NoDaLista> conteudo = new ArrayList<>(nos.length);
        for (int indice = 0; indice < nos.length; indice++) {
            BolsaAed bolsa = nos[indice];
            conteudo.add(new NoDaLista(indice + 1, bolsa.getCodigo(), bolsa.getTipoSanguineo(),
                    bolsa.getComponente(), bolsa.getDataValidade(), bolsa.getStatus(),
                    bolsa.getSequenciaEntrada()));
        }
        return new SnapshotDaLista("ListaEstoque (lista simplesmente encadeada, nos manuais)",
                "ordem de chegada", lista.tamanho(), OffsetDateTime.now(),
                "A ordem e a de chegada. Ordenar por validade seria FEFO, conteudo da Unidade 2.",
                conteudo);
    }

    @Transactional(readOnly = true)
    public SnapshotDaFila fila() {
        FilaRequisicoes fila = requisicaoService.montarFilaAPartirDoBanco();
        RequisicaoAed[] nos = fila.paraVetor();
        List<NoDaFila> conteudo = new ArrayList<>(nos.length);
        for (int indice = 0; indice < nos.length; indice++) {
            RequisicaoAed item = nos[indice];
            conteudo.add(new NoDaFila(indice + 1, item.getCodigo(), item.getHospital(),
                    item.getPrioridade(), item.getSequenciaChegada()));
        }
        return new SnapshotDaFila("FilaRequisicoes (tres filas FIFO encadeadas, uma por prioridade)",
                "EMERGENCIA, depois URGENTE, depois ELETIVA; dentro de cada uma, ordem de chegada",
                fila.tamanho(), OffsetDateTime.now(),
                "Prioridade ordena REQUISICOES. Nao confundir com FEFO, que ordena BOLSAS (Unidade 2).",
                conteudo);
    }

    @Transactional(readOnly = true)
    public SnapshotDaPilha historico() {
        PilhaHistorico pilha = new PilhaHistorico();
        List<EventoRastreabilidade> cronologicos = eventos.listarNaOrdemCronologica();
        int inicio = Math.max(0, cronologicos.size() - LIMITE_DE_EVENTOS_NA_PILHA);
        for (int indice = inicio; indice < cronologicos.size(); indice++) {
            EventoRastreabilidade evento = cronologicos.get(indice);
            pilha.empilhar(new EventoAed(
                    evento.getBolsa().getCodigo(),
                    evento.getStatusAnterior() == null ? null : evento.getStatusAnterior().name(),
                    evento.getStatusNovo().name(),
                    evento.getOcorridoEm().toString()));
        }

        EventoAed[] nos = pilha.paraVetor();
        List<NoDaPilha> conteudo = new ArrayList<>(nos.length);
        for (int indice = 0; indice < nos.length; indice++) {
            EventoAed evento = nos[indice];
            conteudo.add(new NoDaPilha(indice + 1, evento.getCodigoBolsa(),
                    evento.getStatusAnterior(), evento.getStatusNovo(), evento.getOcorridoEm()));
        }
        return new SnapshotDaPilha("PilhaHistorico (pilha encadeada, nos manuais)",
                "LIFO: do evento mais recente para o mais antigo", pilha.tamanho(),
                OffsetDateTime.now(),
                "Representacao em memoria dos ultimos " + LIMITE_DE_EVENTOS_NA_PILHA
                        + " eventos. O historico persistido e imutavel e nao e apagado por desempilhar.",
                conteudo);
    }

    public SituacaoDoModuloAed situacao() {
        return new SituacaoDoModuloAed(
                "U1",
                List.of("ListaEstoque (lista)", "FilaRequisicoes (fila)", "PilhaHistorico (pilha)",
                        "paridade C++ <-> Java das tres estruturas"),
                List.of("MatrizCompatibilidade (ABO/Rh)", "FefoSeletor (insercao ordenada)",
                        "IndiceEstoqueHash (tabela hash manual)", "Roteirizador (vizinho mais proximo)"),
                "Os itens pendentes existem apenas como interface e stub que lanca "
                        + "UnsupportedOperationException. Nenhuma linha deles foi implementada.");
    }
}
