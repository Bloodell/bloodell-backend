package br.edu.cesar.bloodell.indicadores.apresentacao;

import br.edu.cesar.bloodell.indicadores.aplicacao.IndicadorService;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorDemandaResposta;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorEstoqueResposta;
import br.edu.cesar.bloodell.indicadores.apresentacao.IndicadorDtos.IndicadorTemposResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indicadores")
@Tag(name = "Indicadores", description = "Analise descritiva sobre os dados sinteticos do sistema")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping("/estoque")
    @Operation(summary = "Estoque disponivel por tipo e por componente, com medidas descritivas")
    public IndicadorEstoqueResposta estoque() {
        return indicadorService.estoque();
    }

    @GetMapping("/demanda")
    @Operation(summary = "Demanda por hospital e por prioridade")
    public IndicadorDemandaResposta demanda() {
        return indicadorService.demanda();
    }

    @GetMapping("/tempos")
    @Operation(summary = "Tempo entre criacao e atendimento das requisicoes")
    public IndicadorTemposResposta tempos() {
        return indicadorService.tempos();
    }
}
