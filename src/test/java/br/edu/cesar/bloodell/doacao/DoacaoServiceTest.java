package br.edu.cesar.bloodell.doacao;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.doacao.aplicacao.DoacaoService;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.doacao.infraestrutura.DoacaoRepository;
import br.edu.cesar.bloodell.estoque.aplicacao.SincronizadorDeEstoque;
import br.edu.cesar.bloodell.rastreabilidade.aplicacao.RegistradorDeEventos;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DoacaoServiceTest {

    @Test
    @DisplayName("componente repetido na mesma doacao e recusado antes de gravar")
    void naoGeraDuasBolsasDoMesmoComponente() {
        DoacaoRepository doacoes = mock(DoacaoRepository.class);
        BolsaRepository bolsas = mock(BolsaRepository.class);
        UnidadeService unidades = mock(UnidadeService.class);
        Hemocentro hemocentro = new Hemocentro("Hemocentro de Teste",
                new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0578, -34.8829));
        when(unidades.buscarEntidade(1L)).thenReturn(hemocentro);
        when(doacoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(bolsas.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        DoacaoService servico = new DoacaoService(doacoes, bolsas, unidades,
                mock(SincronizadorDeEstoque.class), mock(RegistradorDeEventos.class));

        DoacaoRequisicao entrada = new DoacaoRequisicao(1L, "A+", 450, null,
                List.of(TipoComponente.PLASMA, TipoComponente.PLASMA));

        assertThatThrownBy(() -> servico.registrar(entrada))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("repetido");
        verifyNoInteractions(doacoes, bolsas);
    }
}
