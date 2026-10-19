package br.edu.cesar.bloodell.veiculo.apresentacao;

import br.edu.cesar.bloodell.telemetria.aplicacao.TelemetriaService;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraResposta;
import br.edu.cesar.bloodell.veiculo.aplicacao.VeiculoService;
import br.edu.cesar.bloodell.veiculo.apresentacao.VeiculoDtos.VeiculoRequisicao;
import br.edu.cesar.bloodell.veiculo.apresentacao.VeiculoDtos.VeiculoResposta;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/veiculos")
@Tag(name = "Veiculos", description = "Cadastro da frota de transporte e historico de cadeia fria")
public class VeiculoController {

    private final VeiculoService veiculoService;
    private final TelemetriaService telemetriaService;

    public VeiculoController(VeiculoService veiculoService, TelemetriaService telemetriaService) {
        this.veiculoService = veiculoService;
        this.telemetriaService = telemetriaService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um veiculo na frota de um hemocentro")
    public ResponseEntity<VeiculoResposta> cadastrar(@Valid @RequestBody VeiculoRequisicao entrada) {
        VeiculoResposta criado = veiculoService.cadastrar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/veiculos/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista veiculos, opcionalmente por status")
    public List<VeiculoResposta> listar(@RequestParam(required = false) StatusVeiculo status) {
        return veiculoService.listar(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um veiculo")
    public VeiculoResposta detalhar(@PathVariable Long id) {
        return veiculoService.detalhar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro de um veiculo que nao esteja em rota")
    public VeiculoResposta atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequisicao entrada) {
        return veiculoService.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um veiculo sem entregas ou telemetria registradas")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        veiculoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/telemetrias")
    @Operation(summary = "Historico de telemetria do veiculo no periodo informado")
    public List<LeituraResposta> telemetrias(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return telemetriaService.historicoDoVeiculo(id, de, ate);
    }
}
