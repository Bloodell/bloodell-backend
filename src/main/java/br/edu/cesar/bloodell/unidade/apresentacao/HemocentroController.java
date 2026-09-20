package br.edu.cesar.bloodell.unidade.apresentacao;

import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeRequisicao;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeResposta;
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

@RestController
@RequestMapping("/api/v1/hemocentros")
@Tag(name = "Hemocentros", description = "Cadastro e consulta de hemocentros")
public class HemocentroController {

    private final UnidadeService unidadeService;

    public HemocentroController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista os hemocentros cadastrados")
    public List<UnidadeResposta> listar() {
        return unidadeService.listarHemocentros();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um hemocentro")
    public UnidadeResposta detalhar(@PathVariable Long id) {
        return unidadeService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um hemocentro")
    public ResponseEntity<UnidadeResposta> cadastrar(@Valid @RequestBody UnidadeRequisicao entrada) {
        UnidadeResposta criado = unidadeService.cadastrarHemocentro(entrada);
        return ResponseEntity.created(URI.create("/api/v1/hemocentros/" + criado.id())).body(criado);
    }
}
