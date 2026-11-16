package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
@EnableConfigurationProperties(PropriedadesJwt.class)
public class ConfiguracaoJwt {
    @Bean
    public Clock relogioJwt() { return Clock.systemUTC(); }

    @Bean
    public SecretKey chaveJwt(PropriedadesJwt propriedades) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(propriedades.getSegredoBase64());
        } catch (IllegalArgumentException | NullPointerException erro) {
            throw new IllegalStateException("Configure BLOODELL_JWT_SEGREDO_BASE64 com chave aleatoria em Base64.");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("A chave JWT deve conter pelo menos 32 bytes aleatorios.");
        }
        if (propriedades.getValidade() == null || propriedades.getValidade().compareTo(Duration.ofSeconds(1)) < 0
                || propriedades.getValidade().compareTo(Duration.ofHours(24)) > 0
                || propriedades.getEmissor() == null || propriedades.getEmissor().isBlank()
                || propriedades.getAudiencia() == null || propriedades.getAudiencia().isBlank()) {
            throw new IllegalStateException("JWT exige emissor, audiencia e validade entre 1 segundo e 24 horas.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder emissorJwt(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder decodificadorJwt(SecretKey chaveJwt, PropriedadesJwt propriedades, Clock relogioJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt)
                .macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator datas = new JwtTimestampValidator(Duration.ZERO);
        datas.setClock(relogioJwt);
        OAuth2TokenValidator<Jwt> contrato = jwt -> {
            if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(relogioJwt.instant())
                    || jwt.getIssuedAt() == null || jwt.getIssuedAt().isAfter(relogioJwt.instant())
                    || jwt.getSubject() == null || !jwt.getSubject().matches("[1-9][0-9]{0,18}")
                    || jwt.getAudience() == null || !jwt.getAudience().contains(propriedades.getAudiencia())) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                datas, new JwtIssuerValidator(propriedades.getEmissor()), contrato));
        return decoder;
    }
}
