package br.edu.cesar.bloodell.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroDeCorrelacao extends OncePerRequestFilter {

    public static final String CHAVE_MDC = "requisicaoId";
    public static final String CABECALHO = "X-Correlacao-Id";

    public static String correlacaoAtual() {
        String valor = MDC.get(CHAVE_MDC);
        return valor == null ? "sem-correlacao" : valor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String recebido = requisicao.getHeader(CABECALHO);
        String correlacao = (recebido == null || recebido.isBlank())
                ? UUID.randomUUID().toString().substring(0, 12)
                : recebido.trim();
        MDC.put(CHAVE_MDC, correlacao);
        resposta.setHeader(CABECALHO, correlacao);
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
