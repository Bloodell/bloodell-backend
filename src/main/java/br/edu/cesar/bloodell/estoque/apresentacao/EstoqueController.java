package br.edu.cesar.bloodell.estoque.apresentacao;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.estoque.aplicacao.EstoqueService;
import br.edu.cesar.bloodell.estoque.apresentacao.EstoqueDtos.EstoqueItemResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoques")
@Tag(name = "Estoque", description = "Posicao de estoque por unidade, tipo e componente")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    @Operation(summary = "Posicao de estoque, com filtros opcionais por unidade e componente")
    public List<EstoqueItemResposta> listar(@RequestParam(required = false) Long unidadeId,
                                            @RequestParam(required = false) TipoComponente componente) {
        return estoqueService.listar(unidadeId, componente);
    }
}
