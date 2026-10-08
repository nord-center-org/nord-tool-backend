package br.com.nord_tool_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS por perfil: as origens vêm de {@code nord-tool.cors.allowed-origins} (env NORD_CORS_ORIGINS,
 * separadas por vírgula). Sem credenciais de navegador, porque a autenticação é Bearer no header.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public CorsConfig(@Value("${nord-tool.cors.allowed-origins:}") String[] origensPermitidas) {
        this.origensPermitidas = Arrays.stream(origensPermitidas)
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Request-Id")
                .exposedHeaders("Content-Disposition", "X-Request-Id")
                .allowCredentials(false);
    }
}
