package br.edu.cesar.bloodell.autenticacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.AdminInicial;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminInicialTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder senhas = new BCryptPasswordEncoder(4);

    @Test
    void provisionaSomenteHashEPerfilAdminEmBancoVazio() {
        new AdminInicial(usuarios, senhas, "Admin", "Admin@example.test", "Senha administrativa!").run(null);
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        var usuario = captor.getValue();
        assertThat(usuario.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(usuario.getEmail()).isEqualTo("admin@example.test");
        assertThat(senhas.matches("Senha administrativa!", usuario.getSenhaHash())).isTrue();
    }

    @Test
    void reinicioNaoTrocaSenhaNemPromoveContaExistente() {
        when(usuarios.count()).thenReturn(1L);
        Usuario admin = new Usuario("Admin", "admin@example.test", Perfil.ADMIN, senhas.encode("Senha antiga!"));
        when(usuarios.buscarPorEmail("admin@example.test")).thenReturn(Optional.of(admin));
        new AdminInicial(usuarios, senhas, "Admin", "admin@example.test", "Outra senha!").run(null);
        verify(usuarios, never()).saveAndFlush(any());
        assertThat(senhas.matches("Senha antiga!", admin.getSenhaHash())).isTrue();
        when(usuarios.buscarPorEmail("admin@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new AdminInicial(usuarios, senhas, "Admin", "admin@example.test", "Outra senha!").run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaSenhaCurtaOuMaiorQue72Bytes() {
        for (String senha : new String[] {"", "curta", "á".repeat(40)}) {
            assertThatThrownBy(() -> new AdminInicial(usuarios, senhas, "Admin", "admin@example.test", senha).run(null))
                    .isInstanceOf(IllegalStateException.class);
        }
        verify(usuarios, never()).saveAndFlush(any());
    }
}
