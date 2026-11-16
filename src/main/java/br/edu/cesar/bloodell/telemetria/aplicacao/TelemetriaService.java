package br.edu.cesar.bloodell.telemetria.aplicacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.AcessoUnidade;
import br.edu.cesar.bloodell.compartilhado.dominio.FaixaTemperatura;
import br.edu.cesar.bloodell.compartilhado.excecao.CapacidadeExcedidaException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.config.FiltroDeCorrelacao;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.AceiteResposta;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraEntrada;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraResposta;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.MetricasResposta;
import br.edu.cesar.bloodell.telemetria.dominio.LeituraTelemetria;
import br.edu.cesar.bloodell.telemetria.infraestrutura.FilaDeIngestao;
import br.edu.cesar.bloodell.telemetria.infraestrutura.FilaDeIngestao.LeituraEmTransito;
import br.edu.cesar.bloodell.telemetria.infraestrutura.LeituraTelemetriaRepository;
import br.edu.cesar.bloodell.telemetria.infraestrutura.MetricasDeIngestao;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetriaService {

    private final AcessoUnidade acesso;
    private final br.edu.cesar.bloodell.entrega.infraestrutura.EntregaRepository entregas;

    private static final FaixaTemperatura FAIXA_TRANSPORTE = new FaixaTemperatura(2.0, 6.0);

    private final FilaDeIngestao fila;
    private final MetricasDeIngestao metricas;
    private final LeituraTelemetriaRepository leituras;
    private final VeiculoRepository veiculos;
    private final PropriedadesBloodell propriedades;

    public TelemetriaService(FilaDeIngestao fila,
                             MetricasDeIngestao metricas,
                             LeituraTelemetriaRepository leituras,
                             VeiculoRepository veiculos,
                             PropriedadesBloodell propriedades, AcessoUnidade acesso, br.edu.cesar.bloodell.entrega.infraestrutura.EntregaRepository entregas) {
        this.entregas = entregas;
        this.acesso = acesso;
        this.fila = fila;
        this.metricas = metricas;
        this.leituras = leituras;
        this.veiculos = veiculos;
        this.propriedades = propriedades;
    }

    @Transactional(readOnly = true)
    public AceiteResposta receber(LeituraEntrada entrada) {
        acesso.exigirRecurso(veiculos.findById(entrada.veiculoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Veiculo", entrada.veiculoId())));
        conferirEntrega(entrada.entregaId(), entrada.veiculoId());
        String correlacao = FiltroDeCorrelacao.correlacaoAtual();
        metricas.contarRecebida();

        boolean aceita = fila.oferecer(new LeituraEmTransito(entrada, correlacao));
        if (!aceita) {
            metricas.contarRecusada();
            throw new CapacidadeExcedidaException(
                    "A fila de ingestao de telemetria esta cheia (" + fila.getCapacidade()
                            + " posicoes). Tente novamente em instantes.");
        }
        return new AceiteResposta(correlacao, fila.tamanhoAtual(),
                "Leitura aceita. A gravacao e feita em lote pelos workers de ingestao.");
    }

    @Transactional(readOnly = true)
    public List<LeituraResposta> historicoDoVeiculo(Long veiculoId, OffsetDateTime de, OffsetDateTime ate) {
        acesso.exigirRecurso(veiculos.findById(veiculoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Veiculo", veiculoId)));
        List<LeituraTelemetria> encontradas = leituras.listarPorVeiculoNoPeriodo(veiculoId, de, ate);
        return acesso.filtrar(encontradas).stream()
                .map(leitura -> new LeituraResposta(
                    leitura.getId(),
                    leitura.getVeiculo().getId(),
                    leitura.getPosicao().getLatitude(),
                    leitura.getPosicao().getLongitude(),
                    leitura.getTemperaturaCelsius(),
                    leitura.getStatusVeiculo(),
                    leitura.getRegistradaEm(),
                    leitura.getRecebidaEm(),
                    FAIXA_TRANSPORTE.contem(leitura.getTemperaturaCelsius().doubleValue())))
                .toList();
    }

    private void conferirEntrega(Long entregaId, Long veiculoId) {
        if (entregaId == null) return;
        var entrega = acesso.exigirRecurso(entregas.findById(entregaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Entrega", entregaId)));
        if (!entrega.getVeiculo().getId().equals(veiculoId))
            throw new IllegalArgumentException("Entrega nao pertence ao veiculo informado.");
    }

    public MetricasResposta metricas() {
        acesso.exigirAdministrador();
        return new MetricasResposta(
                metricas.getRecebidas(),
                metricas.getPersistidas(),
                metricas.getRecusadas(),
                metricas.getLotes(),
                metricas.getTamanhoMedioDoLote(),
                metricas.getTempoDeGravacaoMs(),
                fila.tamanhoAtual(),
                fila.getCapacidade(),
                propriedades.getTelemetria().getQuantidadeWorkers());
    }
}
