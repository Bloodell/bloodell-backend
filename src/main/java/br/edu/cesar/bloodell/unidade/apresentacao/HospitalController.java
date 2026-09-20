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
@RequestMapping("/api/v1/hospitais")
@Tag(name = "Hospitais", description = "Cadastro e consulta de hospitais")
public class HospitalController {

    private final UnidadeService unidadeService;

    public HospitalController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista os hospitais cadastrados")
    public List<UnidadeResposta> listar() {
        return unidadeService.listarHospitais();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um hospital")
    public UnidadeResposta detalhar(@PathVariable Long id) {
        return unidadeService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um hospital")
    public ResponseEntity<UnidadeResposta> cadastrar(@Valid @RequestBody UnidadeRequisicao entrada) {
        UnidadeResposta criado = unidadeService.cadastrarHospital(entrada);
        return ResponseEntity.created(URI.create("/api/v1/hospitais/" + criado.id())).body(criado);
    }
}
