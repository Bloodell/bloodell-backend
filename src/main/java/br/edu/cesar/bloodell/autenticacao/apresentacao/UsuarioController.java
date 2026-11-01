package br.edu.cesar.bloodell.autenticacao.apresentacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.CadastroDeUsuarioService;
import br.edu.cesar.bloodell.autenticacao.aplicacao.ConsultaDeUsuarioService;
import br.edu.cesar.bloodell.autenticacao.aplicacao.AtualizacaoDeUsuarioService;
import br.edu.cesar.bloodell.compartilhado.apresentacao.ProblemaApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
@Tag(name = "Usuarios", description = "Cadastro, consulta e atualizacao administrativa de contas da equipe.")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {
    private static final int TAMANHO_MAXIMO_DE_PAGINA = 100;
    private final CadastroDeUsuarioService cadastros;
    private final ConsultaDeUsuarioService consultas;
    private final AtualizacaoDeUsuarioService atualizacoes;

    public UsuarioController(CadastroDeUsuarioService cadastros, ConsultaDeUsuarioService consultas,
            AtualizacaoDeUsuarioService atualizacoes) {
        this.cadastros = cadastros;
        this.consultas = consultas;
        this.atualizacoes = atualizacoes;
    }

    @PostMapping
    @Operation(summary = "Cadastrar usuario", description = "Exige perfil ADMIN. Nao existe cadastro publico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario cadastrado"),
        @ApiResponse(responseCode = "400", description = "Campos ou JSON invalidos", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "403", description = "Usuario autenticado sem perfil ADMIN", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado", content = @Content(schema = @Schema(implementation = ProblemaApi.class)))
    })
    public ResponseEntity<AutenticacaoDtos.UsuarioAtual> cadastrar(@Valid @RequestBody UsuarioDtos.Cadastro entrada) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(cadastros.cadastrar(entrada));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Exige ADMIN. Paginacao por page/size, ordenacao fixa por id crescente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagina de usuarios; content pode ser vazio"),
        @ApiResponse(responseCode = "400", description = "Parametro de paginacao invalido", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "403", description = "Usuario autenticado sem perfil ADMIN", content = @Content(schema = @Schema(implementation = ProblemaApi.class)))
    })
    public ResponseEntity<Page<AutenticacaoDtos.UsuarioAtual>> listar(
            @Parameter(description = "Pagina a partir de 0; negativos sao normalizados para 0.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Padrao 20; normalizado entre 1 e 100.")
            @RequestParam(defaultValue = "20") int size) {
        int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO_DE_PAGINA);
        int pagina = Math.max(page, 0);
        // JPA limita o offset a int; recusa entradas extremas antes da consulta.
        if ((long) pagina * tamanho > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Paginacao excede o limite permitido.");
        }
        var paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.listar(paginacao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario por ID", description = "Exige ADMIN. Retorna apenas id, nome, email e perfil.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "400", description = "Identificador invalido", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "403", description = "Usuario autenticado sem perfil ADMIN", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado", content = @Content(schema = @Schema(implementation = ProblemaApi.class)))
    })
    public ResponseEntity<AutenticacaoDtos.UsuarioAtual> buscar(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.buscar(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar usuario", description = "Exige ADMIN. Atualizacao parcial de nome, email e perfil, sem alterar senha. Preserva o ultimo administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
        @ApiResponse(responseCode = "400", description = "JSON ou campos invalidos; somente nome, email e perfil sao aceitos", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "403", description = "Solicitante sem perfil ADMIN atual", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado", content = @Content(schema = @Schema(implementation = ProblemaApi.class))),
        @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado ou remocao do ultimo administrador", content = @Content(schema = @Schema(implementation = ProblemaApi.class)))
    })
    public ResponseEntity<AutenticacaoDtos.UsuarioAtual> atualizar(@PathVariable Long id,
            @Valid @RequestBody UsuarioDtos.Atualizacao entrada,
            @AuthenticationPrincipal AutenticacaoDtos.UsuarioAtual responsavel) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(atualizacoes.atualizar(id, entrada, responsavel.id()));
    }
}
