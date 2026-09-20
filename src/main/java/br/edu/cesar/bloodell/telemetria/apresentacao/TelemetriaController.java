package br.edu.cesar.bloodell.telemetria.apresentacao;

import br.edu.cesar.bloodell.telemetria.aplicacao.TelemetriaService;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.AceiteResposta;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraEntrada;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.MetricasResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetrias")
@Tag(name = "Telemetria", description = "Ingestao concorrente de leituras sinteticas de veiculo")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;

    public TelemetriaController(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    @PostMapping
    @Operation(summary = "Recebe uma leitura de telemetria; a gravacao e assincrona (202)")
    public ResponseEntity<AceiteResposta> receber(@Valid @RequestBody LeituraEntrada entrada) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(telemetriaService.receber(entrada));
    }

    @GetMapping("/metricas")
    @Operation(summary = "Numeros da ingestao concorrente (evidencia da disciplina de SO)")
    public MetricasResposta metricas() {
        return telemetriaService.metricas();
    }
}
