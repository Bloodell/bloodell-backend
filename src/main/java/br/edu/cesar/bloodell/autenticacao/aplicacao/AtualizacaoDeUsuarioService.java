package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos.UsuarioAtual;
import br.edu.cesar.bloodell.autenticacao.apresentacao.UsuarioDtos.Atualizacao;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.BloqueioDeUsuarios;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
public class AtualizacaoDeUsuarioService {
    private final UsuarioRepository usuarios;
    private final BloqueioDeUsuarios bloqueio;

    public AtualizacaoDeUsuarioService(UsuarioRepository usuarios, BloqueioDeUsuarios bloqueio) {
        this.usuarios = usuarios;
        this.bloqueio = bloqueio;
    }

    @Transactional
    public UsuarioAtual atualizar(Long id, Atualizacao entrada, Long administradorId) {
        bloqueio.bloquearEscritas();
        // O solicitante pode ter perdido o perfil enquanto aguardava outra escrita.
        usuarios.findById(administradorId)
                .filter(u -> u.getPerfil() == Perfil.ADMIN && Usuario.hashBcryptValido(u.getSenhaHash()))
                .orElseThrow(() -> new AccessDeniedException("Somente administradores podem atualizar usuarios."));
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", id));
        if (entrada.getEmail() != null) {
            usuarios.buscarPorEmail(entrada.getEmail()).filter(u -> !u.getId().equals(id)).ifPresent(outro -> {
                throw new ConflitoException("Ja existe um usuario com esse e-mail.");
            });
        }
        if (usuario.getPerfil() == Perfil.ADMIN && Usuario.hashBcryptValido(usuario.getSenhaHash())
                && entrada.getPerfil() != null && entrada.getPerfil() != Perfil.ADMIN
                && usuarios.findByPerfil(Perfil.ADMIN).stream().filter(u -> Usuario.hashBcryptValido(u.getSenhaHash())).count() <= 1) {
            throw new ConflitoException("Nao e permitido remover o perfil do ultimo administrador que pode entrar no sistema.");
        }
        usuario.atualizarCadastro(entrada.getNome(), entrada.getEmail(), entrada.getPerfil());
        usuarios.flush();
        return UsuarioAtual.de(usuario);
    }
}
