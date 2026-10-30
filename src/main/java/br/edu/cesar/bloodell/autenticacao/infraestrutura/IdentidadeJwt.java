package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reconsulta a conta: usuario removido ou sem hash nao mantem acesso pelo token. */
public class IdentidadeJwt implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UsuarioRepository usuarios;

    public IdentidadeJwt(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long id;
        try { id = Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException erro) { throw new OAuth2AuthenticationException("invalid_token"); }
        Usuario usuario = usuarios.findById(id).filter(u -> Usuario.hashBcryptValido(u.getSenhaHash()))
                .orElseThrow(() -> new OAuth2AuthenticationException("invalid_token"));
        return UsernamePasswordAuthenticationToken.authenticated(AutenticacaoDtos.UsuarioAtual.de(usuario), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
    }
}
