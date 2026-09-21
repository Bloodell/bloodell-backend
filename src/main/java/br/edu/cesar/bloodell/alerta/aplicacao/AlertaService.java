package br.edu.cesar.bloodell.alerta.aplicacao;

import br.edu.cesar.bloodell.alerta.dominio.Alerta;
import br.edu.cesar.bloodell.alerta.dominio.NivelAlerta;
import br.edu.cesar.bloodell.alerta.apresentacao.AlertaDtos.AlertaResposta;
import br.edu.cesar.bloodell.alerta.infraestrutura.AlertaRepository;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.telemetria.dominio.LeituraTelemetria;
import br.edu.cesar.bloodell.telemetria.infraestrutura.LeituraTelemetriaRepository;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaService {

    private static final String TIPO_FALHA_COMUNICACAO = "FALHA_COMUNICACAO";

    private final AlertaRepository alertas;
    private final VeiculoRepository veiculos;
    private final LeituraTelemetriaRepository leituras;
    private final PropriedadesBloodell propriedades;

    public AlertaService(AlertaRepository alertas,
                         VeiculoRepository veiculos,
                         LeituraTelemetriaRepository leituras,
                         PropriedadesBloodell propriedades) {
        this.alertas = alertas;
        this.veiculos = veiculos;
        this.leituras = leituras;
        this.propriedades = propriedades;
    }

    @Transactional
    public List<AlertaResposta> listarAtivos() {
        detectarFalhasDeComunicacao();
        List<AlertaResposta> respostas = new ArrayList<>();
        for (Alerta alerta : alertas.listarAbertos()) {
            respostas.add(new AlertaResposta(
                    alerta.getId(),
                    alerta.getTipo(),
                    alerta.getNivel().name(),
                    alerta.getMensagem(),
                    alerta.getVeiculoId(),
                    alerta.getEntregaId(),
                    alerta.getBolsaId(),
                    alerta.getCriadoEm()));
        }
        return respostas;
    }

    private void detectarFalhasDeComunicacao() {
        OffsetDateTime agora = OffsetDateTime.now();
        long limiteSegundos = propriedades.getTelemetria().getTimeoutComunicacaoSegundos();

        for (Veiculo veiculo : veiculos.findByStatusOrderByPlaca(StatusVeiculo.EM_ROTA)) {
            var ultimaLeitura = leituras.findTopByVeiculoIdOrderByRegistradaEmDesc(veiculo.getId());
            boolean semComunicacao = ultimaLeitura.isEmpty()
                    || Duration.between(ultimaLeitura.get().getRegistradaEm(), agora).getSeconds()
                    > limiteSegundos;
            if (!semComunicacao
                    || alertas.existsByTipoAndVeiculoIdAndResolvidoEmIsNull(
                            TIPO_FALHA_COMUNICACAO, veiculo.getId())) {
                continue;
            }

            String detalhe = ultimaLeitura.map(LeituraTelemetria::getRegistradaEm)
                    .map(momento -> "ultima leitura em " + momento)
                    .orElse("nenhuma leitura recebida");
            alertas.save(new Alerta(
                    TIPO_FALHA_COMUNICACAO,
                    NivelAlerta.ATENCAO,
                    "Falha de comunicacao com o veiculo " + veiculo.getPlaca() + ": " + detalhe,
                    veiculo.getId(), null, null));
        }
    }
}
