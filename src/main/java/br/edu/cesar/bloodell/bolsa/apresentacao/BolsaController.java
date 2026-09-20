package br.edu.cesar.bloodell.bolsa.apresentacao;

import br.edu.cesar.bloodell.bolsa.aplicacao.BolsaService;
import br.edu.cesar.bloodell.bolsa.apresentacao.BolsaDtos.BolsaResposta;
import br.edu.cesar.bloodell.bolsa.apresentacao.BolsaDtos.EventoResposta;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bolsas")
@Tag(name = "Bolsas", description = "Consulta de bolsas e linha do tempo de rastreabilidade")
public class BolsaController {

    private static final int TAMANHO_MAXIMO_DE_PAGINA = 100;

    private final BolsaService bolsaService;

    public BolsaController(BolsaService bolsaService) {
        this.bolsaService = bolsaService;
    }

    @GetMapping
    @Operation(summary = "Lista bolsas com filtros e paginacao")
    public Page<BolsaResposta> listar(
            @RequestParam(required = false) EstadoBolsa status,
            @RequestParam(required = false) TipoComponente componente,
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO_DE_PAGINA);
        return bolsaService.listar(status, componente, unidadeId, PageRequest.of(Math.max(page, 0), tamanho));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma bolsa")
    public BolsaResposta detalhar(@PathVariable Long id) {
        return bolsaService.detalhar(id);
    }

    @GetMapping("/{id}/rastreabilidade")
    @Operation(summary = "Linha do tempo completa da bolsa, do evento mais antigo ao mais recente")
    public List<EventoResposta> rastreabilidade(@PathVariable Long id) {
        return bolsaService.rastreabilidade(id);
    }
}
