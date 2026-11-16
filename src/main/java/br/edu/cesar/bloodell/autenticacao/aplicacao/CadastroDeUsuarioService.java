package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos.UsuarioAtual;
import br.edu.cesar.bloodell.autenticacao.apresentacao.UsuarioDtos.Cadastro;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.CadastroDeUsuarioRepository;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
public class CadastroDeUsuarioService {
    private final CadastroDeUsuarioRepository cadastros;
    private final PasswordEncoder senhas;

    public CadastroDeUsuarioService(CadastroDeUsuarioRepository cadastros, PasswordEncoder senhas) {
        this.cadastros = cadastros;
        this.senhas = senhas;
    }

    @Transactional
    public UsuarioAtual cadastrar(Cadastro entrada) {
        Usuario usuario = new Usuario(entrada.nome(), entrada.email(), entrada.perfil(), senhas.encode(entrada.senha()));
        Long id = cadastros.cadastrarSeEmailDisponivel(usuario)
                .orElseThrow(() -> new ConflitoException("Ja existe um usuario com esse e-mail."));
        return new UsuarioAtual(id, usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }
}
