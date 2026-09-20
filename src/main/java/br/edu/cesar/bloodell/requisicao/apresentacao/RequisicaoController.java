package br.edu.cesar.bloodell.requisicao.apresentacao;

import br.edu.cesar.bloodell.alocacao.aplicacao.AlocacaoService;
import br.edu.cesar.bloodell.alocacao.apresentacao.AlocacaoDtos.ResultadoAlocacaoResposta;
import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemDaFilaResposta;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoResposta;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requisicoes")
@Tag(name = "Requisicoes", description = "Pedidos hospitalares, fila de atendimento e alocacao")
public class RequisicaoController {

    private static final int TAMANHO_MAXIMO_DE_PAGINA = 100;

    private final RequisicaoService requisicaoService;
    private final AlocacaoService alocacaoService;

    public RequisicaoController(RequisicaoService requisicaoService, AlocacaoService alocacaoService) {
        this.requisicaoService = requisicaoService;
        this.alocacaoService = alocacaoService;
    }

    @PostMapping
    @Operation(summary = "Cria uma requisicao hospitalar e a coloca na fila")
    public ResponseEntity<RequisicaoResposta> criar(@Valid @RequestBody RequisicaoEntrada entrada) {
        RequisicaoResposta criada = requisicaoService.criar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/requisicoes/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista requisicoes com filtros e paginacao")
    public Page<RequisicaoResposta> listar(
            @RequestParam(required = false) StatusRequisicao status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO_DE_PAGINA);
        return requisicaoService.listar(status, prioridade, hospitalId,
                PageRequest.of(Math.max(page, 0), tamanho));
    }

    @GetMapping("/fila")
    @Operation(summary = "Estado atual da fila de atendimento, na ordem produzida pela estrutura da AED-U1")
    public List<ItemDaFilaResposta> fila() {
        return requisicaoService.estadoDaFila();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma requisicao")
    public RequisicaoResposta detalhar(@PathVariable Long id) {
        return requisicaoService.detalhar(id);
    }

    @PostMapping("/{id}/alocacoes")
    @Operation(summary = "Processa a requisicao e aloca bolsas (na U1, pela regra provisoria)")
    public ResponseEntity<ResultadoAlocacaoResposta> alocar(@PathVariable Long id) {
        ResultadoAlocacaoResposta resultado = alocacaoService.processar(id);
        return ResponseEntity.created(URI.create("/api/v1/requisicoes/" + id)).body(resultado);
    }
}
