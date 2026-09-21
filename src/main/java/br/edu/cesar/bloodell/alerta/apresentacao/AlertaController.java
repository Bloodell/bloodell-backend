package br.edu.cesar.bloodell.alerta.apresentacao;

import br.edu.cesar.bloodell.alerta.aplicacao.AlertaService;
import br.edu.cesar.bloodell.alerta.apresentacao.AlertaDtos.AlertaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas", description = "Alertas ativos da cadeia fria e da operacao")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Lista alertas ativos e verifica falhas recentes de comunicacao")
    public List<AlertaResposta> listarAtivos() {
        return alertaService.listarAtivos();
    }
}
