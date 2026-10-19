package br.edu.cesar.bloodell.doacao.apresentacao;

import br.edu.cesar.bloodell.doacao.aplicacao.DoacaoService;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Doacao nao tem PUT nem DELETE de proposito: cada doacao origina bolsas com trilha
 * de rastreabilidade, e alterar ou apagar a origem quebraria essa trilha.
 */
@RestController
@RequestMapping("/api/v1/doacoes")
@Tag(name = "Doacoes", description = "Registro de doacoes sinteticas e geracao de bolsas")
public class DoacaoController {

    private final DoacaoService doacaoService;

    public DoacaoController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @PostMapping
    @Operation(summary = "Registra uma doacao e gera as bolsas dos componentes informados")
    public ResponseEntity<DoacaoResposta> registrar(@Valid @RequestBody DoacaoRequisicao entrada) {
        DoacaoResposta criada = doacaoService.registrar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/doacoes/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista as doacoes, das mais recentes para as mais antigas")
    public List<DoacaoResposta> listar() {
        return doacaoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma doacao e as bolsas geradas a partir dela")
    public DoacaoResposta detalhar(@PathVariable Long id) {
        return doacaoService.detalhar(id);
    }
}
