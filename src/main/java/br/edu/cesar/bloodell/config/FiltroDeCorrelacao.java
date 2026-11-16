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
    private static final int TAMANHO_MAXIMO = 40;
    private static final java.util.regex.Pattern FORMATO = java.util.regex.Pattern.compile("[A-Za-z0-9._-]{1,40}");

    /** Returns a bounded, header/log/database-safe value even for non-HTTP callers. */
    public static String normalizar(String valor) {
        if (valor != null) {
            String limpo = valor.trim();
            if (limpo.length() <= TAMANHO_MAXIMO && FORMATO.matcher(limpo).matches()) return limpo;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String correlacaoAtual() {
        String valor = MDC.get(CHAVE_MDC);
        return valor == null ? "sem-correlacao" : valor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String correlacao = normalizar(requisicao.getHeader(CABECALHO));
        MDC.put(CHAVE_MDC, correlacao);
        resposta.setHeader(CABECALHO, correlacao);
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
