package br.edu.cesar.bloodell.doacao.apresentacao;

import br.edu.cesar.bloodell.doacao.aplicacao.DoacaoService;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
