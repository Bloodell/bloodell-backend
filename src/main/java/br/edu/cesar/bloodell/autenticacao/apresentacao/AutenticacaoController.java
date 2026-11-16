package br.edu.cesar.bloodell.autenticacao.apresentacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.AutenticacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
public class AutenticacaoController {
    private final AutenticacaoService autenticacao;

    public AutenticacaoController(AutenticacaoService autenticacao) { this.autenticacao = autenticacao; }

    @PostMapping("/login")
    public ResponseEntity<AutenticacaoDtos.Token> login(@Valid @RequestBody AutenticacaoDtos.Login entrada) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autenticacao.entrar(entrada.email(), entrada.senha()));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AutenticacaoDtos.UsuarioAtual> me(
            @AuthenticationPrincipal AutenticacaoDtos.UsuarioAtual usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usuario);
    }
}
