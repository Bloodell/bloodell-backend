package br.edu.cesar.bloodell.entrega.apresentacao;

import br.edu.cesar.bloodell.entrega.aplicacao.EntregaService;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaEntrada;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.EntregaResposta;
import br.edu.cesar.bloodell.entrega.apresentacao.EntregaDtos.PlanejamentoEntregaEntrada;
import br.edu.cesar.bloodell.entrega.dominio.StatusEntrega;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/v1/entregas")
@Tag(name = "Entregas", description = "Planejamento e acompanhamento das entregas de hemocomponentes")
public class EntregaController {

    private final EntregaService entregas;

    public EntregaController(EntregaService entregas) {
        this.entregas = entregas;
    }

    @PostMapping
    @Operation(summary = "Planeja uma entrega para uma requisicao alocada")
    public ResponseEntity<EntregaResposta> criar(@Valid @RequestBody EntregaEntrada entrada) {
        EntregaResposta criada = entregas.criar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/entregas/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista entregas, opcionalmente por status")
    public List<EntregaResposta> listar(@RequestParam(required = false) StatusEntrega status) {
        return entregas.listar(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma entrega")
    public EntregaResposta detalhar(@PathVariable Long id) {
        return entregas.detalhar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o planejamento de uma entrega ainda planejada")
    public EntregaResposta atualizar(@PathVariable Long id,
                                     @Valid @RequestBody PlanejamentoEntregaEntrada entrada) {
        return entregas.atualizarPlanejamento(id, entrada);
    }

    @PostMapping("/{id}/roteirizacao")
    @Operation(summary = "Calcula e persiste a rota geografica de uma entrega planejada")
    public EntregaResposta roteirizar(@PathVariable Long id) {
        return entregas.roteirizar(id);
    }

    @PostMapping("/{id}/despacho")
    @Operation(summary = "Despacha a entrega e coloca requisicao e veiculo em rota")
    public EntregaResposta despachar(@PathVariable Long id) {
        return entregas.despachar(id);
    }

    @PostMapping("/{id}/inicio-transporte")
    @Operation(summary = "Registra o inicio do transporte")
    public EntregaResposta iniciarTransporte(@PathVariable Long id) {
        return entregas.iniciarTransporte(id);
    }

    @PostMapping("/{id}/conclusao")
    @Operation(summary = "Conclui a entrega e atende a requisicao")
    public EntregaResposta concluir(@PathVariable Long id) {
        return entregas.concluir(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela uma entrega que ainda nao foi concluida")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        entregas.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
