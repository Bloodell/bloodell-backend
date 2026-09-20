package br.edu.cesar.bloodell.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PontoDeExtensaoDeAutenticacao extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(PontoDeExtensaoDeAutenticacao.class);

    private final PropriedadesBloodell propriedades;

    public PontoDeExtensaoDeAutenticacao(PropriedadesBloodell propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        if (propriedades.getSeguranca().isAutenticacaoHabilitada()) {
            autenticar(requisicao);
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(HttpServletRequest requisicao) {
        LOG.warn("bloodell.seguranca.autenticacao-habilitada=true, mas a autenticacao ainda nao foi "
                + "implementada (fora do MVP). Requisicao {} seguiu sem verificacao de identidade.",
                requisicao.getRequestURI());
    }
}
