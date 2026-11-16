package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Provisionamento administrativo explicito; nao existe endpoint de cadastro publico. */
@Component
@ConditionalOnProperty(name = {"bloodell.seguranca.autenticacao-habilitada",
        "bloodell.seguranca.admin-inicial.habilitado"}, havingValue = "true")
public class AdminInicial implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder senhas,
            @Value("${bloodell.seguranca.admin-inicial.nome:}") String nome,
            @Value("${bloodell.seguranca.admin-inicial.email:}") String email,
            @Value("${bloodell.seguranca.admin-inicial.senha:}") String senha) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.count() > 0) {
            if (usuarios.buscarPorEmail(Usuario.normalizarEmail(email))
                    .filter(u -> u.getPerfil() == Perfil.ADMIN && Usuario.hashBcryptValido(u.getSenhaHash()))
                    .isPresent()) {
                return; // Nunca troca senha nem promove uma conta ja existente.
            }
            throw new IllegalStateException("Provisionamento inicial exige banco sem usuarios; revise as contas existentes.");
        }
        int bytes = senha.getBytes(StandardCharsets.UTF_8).length;
        if (senha.isBlank() || senha.length() < 12 || bytes > 72 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("Configure e-mail e senha do admin inicial (12 caracteres a 72 bytes UTF-8).");
        }
        usuarios.saveAndFlush(new Usuario(nome, email, Perfil.ADMIN, senhas.encode(senha)));
    }
}
