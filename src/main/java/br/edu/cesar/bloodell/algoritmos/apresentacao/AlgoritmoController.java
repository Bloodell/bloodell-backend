package br.edu.cesar.bloodell.algoritmos.apresentacao;

import br.edu.cesar.bloodell.algoritmos.aplicacao.AlgoritmoService;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SituacaoDoModuloAed;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaFila;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaLista;
import br.edu.cesar.bloodell.algoritmos.apresentacao.AlgoritmoDtos.SnapshotDaPilha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/algoritmos")
@Tag(name = "Algoritmos (AED)", description = "Estado das estruturas de dados implementadas a mao")
public class AlgoritmoController {

    private final AlgoritmoService algoritmoService;

    public AlgoritmoController(AlgoritmoService algoritmoService) {
        this.algoritmoService = algoritmoService;
    }

    @GetMapping("/estoque")
    @Operation(summary = "Retrato da lista encadeada de estoque, na ordem de chegada")
    public SnapshotDaLista estoque(@RequestParam(required = false) Long unidadeId) {
        return algoritmoService.estoque(unidadeId);
    }

    @GetMapping("/fila")
    @Operation(summary = "Retrato da fila de requisicoes, na ordem de atendimento")
    public SnapshotDaFila fila() {
        return algoritmoService.fila();
    }

    @GetMapping("/historico")
    @Operation(summary = "Retrato da pilha de historico, do evento mais recente para o mais antigo")
    public SnapshotDaPilha historico() {
        return algoritmoService.historico();
    }

    @GetMapping("/situacao")
    @Operation(summary = "O que ja esta implementado na AED e o que continua como stub da Unidade 2")
    public SituacaoDoModuloAed situacao() {
        return algoritmoService.situacao();
    }
}
