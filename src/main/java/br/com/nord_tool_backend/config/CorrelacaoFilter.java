package br.com.nord_tool_backend.config;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identificador de correlação por requisição: reaproveita o header {@code X-Request-Id} quando vier em formato
 * seguro, senão gera um UUID. Fica no MDC (aparece em todo log da requisição), no header da resposta e no
 * corpo das respostas de erro, para ligar o que o usuário vê ao que está no log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacaoFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern FORMATO_SEGURO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = idDa(request.getHeader(HEADER));
        MDC.put(ApiResponseBody.MDC_ID_CORRELACAO, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(ApiResponseBody.MDC_ID_CORRELACAO);
        }
    }

    static String idDa(String recebido) {
        if (recebido != null && FORMATO_SEGURO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
