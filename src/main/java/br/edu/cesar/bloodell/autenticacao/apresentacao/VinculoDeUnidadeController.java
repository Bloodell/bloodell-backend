package br.edu.cesar.bloodell.autenticacao.apresentacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.VinculoDeUnidadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios/{id}/unidades")
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
public class VinculoDeUnidadeController {
    private final VinculoDeUnidadeService vinculos;
    public VinculoDeUnidadeController(VinculoDeUnidadeService vinculos) { this.vinculos = vinculos; }

    public record Vinculos(@NotNull Set<@NotNull @Positive Long> unidadeIds) { }

    @GetMapping
    public ResponseEntity<Vinculos> consultar(@PathVariable Long id) {
        return resposta(vinculos.consultar(id));
    }

    /** Substitui todos os vinculos; conjunto vazio revoga todo acesso a unidades. */
    @PutMapping
    public ResponseEntity<Vinculos> substituir(@PathVariable Long id, @Valid @RequestBody Vinculos entrada) {
        return resposta(vinculos.substituir(id, entrada.unidadeIds()));
    }

    private ResponseEntity<Vinculos> resposta(Set<Long> ids) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Vinculos(ids));
    }
}
