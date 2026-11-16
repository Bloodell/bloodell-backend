package br.edu.cesar.bloodell.autenticacao;

import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import br.edu.cesar.bloodell.requisicao.infraestrutura.RequisicaoRepository;
import br.edu.cesar.bloodell.requisicao.dominio.*;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.dominio.Hospital;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsolamentoDeRequisicaoTest {
    @Mock RequisicaoRepository repositorio;
    @Mock UnidadeService unidades;
    RequisicaoService servico;
    @org.junit.jupiter.api.BeforeEach void preparar() {
        var acesso = new br.edu.cesar.bloodell.autenticacao.aplicacao.AcessoUnidade(
                new br.edu.cesar.bloodell.config.PropriedadesBloodell(),
                mock(br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository.class),
                mock(br.edu.cesar.bloodell.autenticacao.infraestrutura.VinculoDeUnidadeRepository.class),
                mock(jakarta.persistence.EntityManager.class));
        servico = new RequisicaoService(repositorio, unidades, acesso);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test void consultaPorIdNaoPodeIgnorarAutorizacaoDaUnidade() {
        Hospital hospital = mock(Hospital.class);
        Requisicao requisicao = new Requisicao("REQ-OUTRA", hospital, Prioridade.ELETIVA);
        when(repositorio.findById(22L)).thenReturn(Optional.of(requisicao));
        assertThrows(AccessDeniedException.class, () -> servico.detalhar(22L));
    }
}
