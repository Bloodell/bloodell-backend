package br.edu.cesar.bloodell.autenticacao.apresentacao;

import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AutenticacaoDtos {
    private AutenticacaoDtos() { }

    public record Login(@NotBlank @Email @Size(max = 160) String email,
                        @NotBlank @Size(max = 72) String senha) {
        @Override
        public String toString() { return "Login[credenciais omitidas]"; }
    }

    public record UsuarioAtual(Long id, String nome, String email, Perfil perfil) {
        public static UsuarioAtual de(Usuario usuario) {
            return new UsuarioAtual(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
        }
    }

    public record Token(String accessToken, String tokenType, long expiresIn, UsuarioAtual usuario) {
        @Override
        public String toString() { return "Token[credenciais omitidas]"; }
    }
}
