package br.com.nord_tool_backend.config;

import br.com.nord_tool_backend.security.UsuarioAtualProvider;
import br.com.nord_tool_backend.service.NotificacaoService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * Registra cada alteração bem-sucedida (POST, PUT, PATCH, DELETE com resposta 2xx) de quem está logado, para avisar
 * os administradores de quem alterou o quê. O aviso roda fora da requisição e nunca a atrasa nem a derruba.
 */
@Configuration
public class AtividadeConfig implements WebMvcConfigurer {

    private static final Set<String> ALTERAM = Set.of("POST", "PUT", "PATCH", "DELETE");

    /** Buscados na hora: nos testes de fatia web (sem serviços) o registro simplesmente não acontece. */
    private final ObjectProvider<NotificacaoService> notificacoes;
    private final ObjectProvider<UsuarioAtualProvider> usuarioAtual;

    public AtividadeConfig(ObjectProvider<NotificacaoService> notificacoes, ObjectProvider<UsuarioAtualProvider> usuarioAtual) {
        this.notificacoes = notificacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                if (ex != null || !ALTERAM.contains(request.getMethod())) return;
                int status = response.getStatus();
                if (status < 200 || status >= 300) return;
                try {
                    NotificacaoService servico = notificacoes.getIfAvailable();
                    UsuarioAtualProvider atual = usuarioAtual.getIfAvailable();
                    if (servico == null || atual == null) return;
                    atual.atual().ifPresent(u -> servico.atividade(u, request.getMethod(), request.getRequestURI()));
                } catch (RuntimeException e) {
                    // aviso nunca derruba a requisição
                }
            }
        }).addPathPatterns("/api/v1/nord-tool/**");
    }
}
