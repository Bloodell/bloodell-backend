package br.edu.cesar.bloodell.telemetria.aplicacao;

import br.edu.cesar.bloodell.alerta.dominio.Alerta;
import br.edu.cesar.bloodell.alerta.dominio.NivelAlerta;
import br.edu.cesar.bloodell.alerta.infraestrutura.AlertaRepository;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.FaixaTemperatura;
import br.edu.cesar.bloodell.telemetria.dominio.LeituraTelemetria;
import br.edu.cesar.bloodell.telemetria.infraestrutura.FilaDeIngestao.LeituraEmTransito;
import br.edu.cesar.bloodell.telemetria.infraestrutura.LeituraTelemetriaRepository;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GravadorDeLeituras {

    private static final Logger LOG = LoggerFactory.getLogger(GravadorDeLeituras.class);

    private static final FaixaTemperatura FAIXA_TRANSPORTE = new FaixaTemperatura(2.0, 6.0);

    private final LeituraTelemetriaRepository leituras;
    private final VeiculoRepository veiculos;
    private final AlertaRepository alertas;

    public GravadorDeLeituras(LeituraTelemetriaRepository leituras,
                              VeiculoRepository veiculos,
                              AlertaRepository alertas) {
        this.leituras = leituras;
        this.veiculos = veiculos;
        this.alertas = alertas;
    }

    @Transactional
    public int gravarLote(List<LeituraEmTransito> lote) {
        List<LeituraTelemetria> paraSalvar = new ArrayList<>(lote.size());

        for (LeituraEmTransito item : lote) {
            Optional<Veiculo> encontrado = veiculos.findById(item.leitura().veiculoId());
            if (encontrado.isEmpty()) {
                LOG.warn("Leitura descartada: veiculo {} nao existe (correlacao {}).",
                        item.leitura().veiculoId(), item.correlacao());
                continue;
            }
            Veiculo veiculo = encontrado.get();
            CoordenadaGeo posicao = new CoordenadaGeo(item.leitura().lat(), item.leitura().lon());
            OffsetDateTime momento = item.leitura().registradaEm() == null
                    ? OffsetDateTime.now()
                    : item.leitura().registradaEm();

            paraSalvar.add(new LeituraTelemetria(veiculo, item.leitura().entregaId(), posicao,
                    item.leitura().temperaturaC(), item.leitura().statusVeiculo(), momento,
                    item.correlacao()));

            veiculo.atualizarPosicao(posicao);

            double temperatura = item.leitura().temperaturaC().doubleValue();
            if (!FAIXA_TRANSPORTE.contem(temperatura)) {
                alertas.save(new Alerta("CADEIA_FRIA", NivelAlerta.CRITICO,
                        "Temperatura " + temperatura + " C fora da faixa didatica "
                                + FAIXA_TRANSPORTE + " no veiculo " + veiculo.getPlaca(),
                        veiculo.getId(), item.leitura().entregaId(), null));
                LOG.warn("Alerta de cadeia fria: veiculo {} a {} C (correlacao {}).",
                        veiculo.getPlaca(), temperatura, item.correlacao());
            }
        }

        leituras.saveAll(paraSalvar);
        return paraSalvar.size();
    }
}
