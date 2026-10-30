package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.PropriedadesJwt;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
public class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder tokens;
    private final PropriedadesJwt propriedades;
    private final Clock relogio;
    private final String hashInexistente;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder tokens,
                               PropriedadesJwt propriedades, Clock relogio) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.propriedades = propriedades;
        this.relogio = relogio;
        this.hashInexistente = senhas.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AutenticacaoDtos.Token entrar(String email, String senha) {
        if (senha == null || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("E-mail ou senha invalidos.");
        }
        Usuario usuario = usuarios.buscarPorEmail(Usuario.normalizarEmail(email)).orElse(null);
        boolean possuiHash = usuario != null && Usuario.hashBcryptValido(usuario.getSenhaHash());
        boolean confere = senhas.matches(senha, possuiHash ? usuario.getSenhaHash() : hashInexistente);
        if (!possuiHash || !confere) {
            throw new BadCredentialsException("E-mail ou senha invalidos.");
        }
        Instant agora = relogio.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(propriedades.getEmissor())
                .audience(List.of(propriedades.getAudiencia())).subject(usuario.getId().toString())
                .issuedAt(agora).expiresAt(agora.plus(propriedades.getValidade()))
                .id(UUID.randomUUID().toString()).claim("perfil", usuario.getPerfil().name()).build();
        String token = tokens.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AutenticacaoDtos.Token(token, "Bearer", propriedades.getValidade().toSeconds(),
                AutenticacaoDtos.UsuarioAtual.de(usuario));
    }
}
