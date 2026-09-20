package br.edu.cesar.bloodell.demonstracao.apresentacao;

import br.edu.cesar.bloodell.demonstracao.aplicacao.SeedDemonstracaoService;
import br.edu.cesar.bloodell.demonstracao.apresentacao.DemonstracaoDtos.CenarioEntrada;
import br.edu.cesar.bloodell.demonstracao.apresentacao.DemonstracaoDtos.CenarioResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demonstracoes/cenarios")
@Tag(name = "Demonstracao", description = "Cenario sintetico reproduzivel para demo e ambiente local")
public class DemonstracaoController {

    private final SeedDemonstracaoService seedService;

    public DemonstracaoController(SeedDemonstracaoService seedService) {
        this.seedService = seedService;
    }

    @PostMapping
    @Operation(summary = "Cria o cenario sintetico deterministico (mesma semente, mesmos dados)")
    public ResponseEntity<CenarioResposta> criar(@Valid @RequestBody CenarioEntrada entrada) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seedService.popular(entrada));
    }
}
