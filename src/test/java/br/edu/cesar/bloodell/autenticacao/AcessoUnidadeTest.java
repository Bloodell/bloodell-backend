package br.edu.cesar.bloodell.autenticacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.AcessoUnidade;
import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos.UsuarioAtual;
import br.edu.cesar.bloodell.autenticacao.dominio.*;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.*;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.unidade.dominio.*;
import br.edu.cesar.bloodell.requisicao.dominio.*;
import br.edu.cesar.bloodell.entrega.dominio.Entrega;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import br.edu.cesar.bloodell.alerta.dominio.Alerta;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcessoUnidadeTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final VinculoDeUnidadeRepository vinculos = mock(VinculoDeUnidadeRepository.class);
    private final EntityManager entidades = mock(EntityManager.class);
    private final PropriedadesBloodell propriedades = new PropriedadesBloodell();
    private final AcessoUnidade acesso = new AcessoUnidade(propriedades, usuarios, vinculos, entidades);
    private final Usuario usuario = new Usuario("Operador", "op@example.test", Perfil.LOGISTICA,
            "$2a$12$" + "a".repeat(53));

    @BeforeEach void preparar() {
        SecurityContextHolder.clearContext();
        org.springframework.test.util.ReflectionTestUtils.setField(usuario, "id", 7L);
        // Uma autoridade/alegacao ADMIN antiga nao pode substituir o perfil atual do banco.
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioAtual(7L, "Operador", "op@example.test", Perfil.ADMIN), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(vinculos.unidadesDoUsuario(7L)).thenReturn(Set.of(10L, 20L));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test void exigeTodosOsVinculosERecusaUnidadeAusente() {
        assertThatCode(() -> acesso.exigir(10L, 20L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> acesso.exigir(10L, 30L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> acesso.exigir((Long) null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(acesso::exigirAdministrador).isInstanceOf(AccessDeniedException.class);
    }

    @Test void revogacaoValeParaMesmaIdentidadeJaAutenticada() {
        acesso.exigir(10L);
        when(vinculos.unidadesDoUsuario(7L)).thenReturn(Set.of());
        assertThatThrownBy(() -> acesso.exigir(10L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void adminAtualDoBancoTemAcessoGlobal() {
        usuario.atualizarCadastro(null, null, Perfil.ADMIN);
        assertThatCode(() -> acesso.exigir(999L)).doesNotThrowAnyException();
        assertThatCode(acesso::exigirAdministrador).doesNotThrowAnyException();
    }

    @Test void contextoAusenteOuContaRemovidaNaoAbreAcesso() {
        when(usuarios.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> acesso.exigir(10L)).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> acesso.filtrar(List.of())).isInstanceOf(AccessDeniedException.class);
    }

    @Test void modoDemonstrativoContinuaExplicito() {
        propriedades.getSeguranca().setAutenticacaoHabilitada(false);
        SecurityContextHolder.clearContext();
        assertThatCode(() -> acesso.exigir(999L)).doesNotThrowAnyException();
    }

    @Test void entregaExigeHospitalOrigemEBaseDoVeiculo() {
        var hospital = unidade(10L);
        var origem = unidade(20L);
        var baseAlheia = unidade(30L);
        var pedido = new Requisicao("REQ-1", hospital, Prioridade.ELETIVA);
        Veiculo veiculo = mock(Veiculo.class);
        when(veiculo.getHemocentroBase()).thenReturn(baseAlheia);
        var entrega = new Entrega(pedido, veiculo, origem, null, null, null);
        assertThat(acesso.filtrar(List.of(entrega))).isEmpty();
        assertThatThrownBy(() -> acesso.exigirRecurso(entrega)).isInstanceOf(AccessDeniedException.class);
        when(vinculos.unidadesDoUsuario(7L)).thenReturn(Set.of(10L, 20L, 30L));
        assertThat(acesso.exigirRecurso(entrega)).isSameAs(entrega);
    }

    @Test void historicoDeBolsaNaoExpoeRequisicaoDeHospitalNaoVinculado() {
        Bolsa bolsa = mock(Bolsa.class);
        var hemocentro = unidade(20L);
        when(bolsa.getUnidadeAtual()).thenReturn(hemocentro);
        var pedido = new Requisicao("REQ-OUTRA", unidade(30L), Prioridade.ELETIVA);
        when(entidades.find(Requisicao.class, 90L)).thenReturn(pedido);
        var evento = new EventoRastreabilidade(bolsa, null, null, 90L, "Hospital alheio");
        assertThat(acesso.filtrar(List.of(evento))).isEmpty();
        assertThat(acesso.exigirRecurso(bolsa)).isSameAs(bolsa);
    }

    @Test void alertasSemDonoOuComReferenciaInexistenteNaoVazam() {
        var semDono = new Alerta("X", null, "global", null, null, null);
        var quebrado = new Alerta("X", null, "inexistente", 999L, null, null);
        assertThat(acesso.filtrar(List.of(semDono, quebrado))).isEmpty();
    }

    private Unidade unidade(Long id) {
        Unidade u = mock(Unidade.class);
        when(u.getId()).thenReturn(id);
        return u;
    }
}
