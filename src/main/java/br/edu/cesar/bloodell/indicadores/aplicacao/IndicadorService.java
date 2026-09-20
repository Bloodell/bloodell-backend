package br.edu.cesar.bloodell.indicadores.aplicacao;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.ContagemPorCategoria;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorDemandaResposta;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorEstoqueResposta;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorTemposResposta;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndicadorService {

    private static final String AVISO =
            "Dados 100% sinteticos, gerados pelo proprio sistema. Sem informacao real de doador ou paciente.";

    private final BolsaRepository bolsas;
    private final RequisicaoRepository requisicoes;

    public IndicadorService(BolsaRepository bolsas, RequisicaoRepository requisicoes) {
        this.bolsas = bolsas;
        this.requisicoes = requisicoes;
    }

    @Transactional(readOnly = true)
    public IndicadorEstoqueResposta estoque() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        Map<String, Long> porComponente = new LinkedHashMap<>();
        long total = 0;

        for (Bolsa bolsa : bolsas.listarTodasNaOrdemDeChegada()) {
            if (bolsa.getStatus() != EstadoBolsa.DISPONIVEL) {
                continue;
            }
            total++;
            porTipo.merge(bolsa.getTipoSanguineo().formatado(), 1L, Long::sum);
            porComponente.merge(bolsa.getComponente().name(), 1L, Long::sum);
        }

        return new IndicadorEstoqueResposta(AVISO, total, converter(porTipo), converter(porComponente),
                EstatisticaDescritiva.de(valores(porTipo)));
    }

    @Transactional(readOnly = true)
    public IndicadorDemandaResposta demanda() {
        Map<String, Long> porHospital = new LinkedHashMap<>();
        Map<String, Long> porPrioridade = new LinkedHashMap<>();
        List<Requisicao> todas = requisicoes.findAll();

        for (Requisicao requisicao : todas) {
            porHospital.merge(requisicao.getHospital().getNome(), 1L, Long::sum);
            porPrioridade.merge(requisicao.getPrioridade().name(), 1L, Long::sum);
        }

        return new IndicadorDemandaResposta(AVISO, todas.size(), converter(porHospital),
                converter(porPrioridade), EstatisticaDescritiva.de(valores(porHospital)));
    }

    @Transactional(readOnly = true)
    public IndicadorTemposResposta tempos() {
        List<Double> minutos = new ArrayList<>();
        long atendidas = 0;

        for (Requisicao requisicao : requisicoes.findAll()) {
            if (requisicao.getAtendidaEm() == null) {
                continue;
            }
            atendidas++;
            double duracao = Duration.between(requisicao.getCriadaEm(), requisicao.getAtendidaEm()).toMillis()
                    / 60000.0;
            minutos.add(duracao);
        }

        double[] valores = new double[minutos.size()];
        for (int indice = 0; indice < minutos.size(); indice++) {
            valores[indice] = minutos.get(indice);
        }

        return new IndicadorTemposResposta(AVISO, "minutos", atendidas,
                requisicoes.countByStatus(StatusRequisicao.NA_FILA),
                EstatisticaDescritiva.de(valores));
    }

    private List<ContagemPorCategoria> converter(Map<String, Long> contagens) {
        List<ContagemPorCategoria> lista = new ArrayList<>(contagens.size());
        for (Map.Entry<String, Long> entrada : contagens.entrySet()) {
            lista.add(new ContagemPorCategoria(entrada.getKey(), entrada.getValue()));
        }
        return lista;
    }

    private double[] valores(Map<String, Long> contagens) {
        double[] numeros = new double[contagens.size()];
        int indice = 0;
        for (Long valor : contagens.values()) {
            numeros[indice] = valor;
            indice++;
        }
        return numeros;
    }
}
