package br.edu.cesar.bloodell.config;

import br.edu.cesar.bloodell.autenticacao.infraestrutura.IdentidadeJwt;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.compartilhado.apresentacao.ProblemaApi;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ConfiguracaoSeguranca {
    @Bean
    public PasswordEncoder codificadorDeSenha() { return new BCryptPasswordEncoder(12); }

    @Bean
    public AuthenticationEntryPoint respostaNaoAutenticada(ObjectMapper mapper) {
        return (requisicao, resposta, erro) -> {
            resposta.setStatus(401);
            resposta.setContentType("application/problem+json");
            resposta.setHeader("WWW-Authenticate", "Bearer");
            resposta.setHeader("Cache-Control", "no-store");
            resposta.getWriter().write(mapper.writeValueAsString(ProblemaApi.de("nao-autenticado",
                    "Autenticacao necessaria", 401, "Token ausente, invalido ou expirado.",
                    requisicao.getRequestURI(), FiltroDeCorrelacao.correlacaoAtual(), null)));
        };
    }

    @Bean
    public SecurityFilterChain seguranca(HttpSecurity http, PropriedadesBloodell propriedades,
            ObjectProvider<JwtDecoder> decoder, UsuarioRepository usuarios,
            AuthenticationEntryPoint respostaNaoAutenticada) throws Exception {
        // Credenciais apenas no Authorization Bearer; sem sessao/cookie de autenticacao.
        http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(erros -> erros.authenticationEntryPoint(respostaNaoAutenticada));
        if (propriedades.getSeguranca().isAutenticacaoHabilitada()) {
            http.authorizeHttpRequests(rotas -> rotas
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                            "/swagger-ui.html", "/swagger-ui/**", "/api/v1/openapi", "/api/v1/openapi/**").permitAll()
                    .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(respostaNaoAutenticada)
                            .jwt(jwt -> jwt.decoder(decoder.getObject())
                                    .jwtAuthenticationConverter(new IdentidadeJwt(usuarios))));
        } else {
            // Compatibilidade temporaria com o frontend do MVP. Auth nunca abre anonimamente.
            http.authorizeHttpRequests(rotas -> rotas.requestMatchers("/api/v1/auth/**").denyAll()
                    .anyRequest().permitAll());
        }
        return http.build();
    }
}
