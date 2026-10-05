package br.com.nord_tool_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração de segurança. Chave de liga/desliga: {@code nord-tool.security.enabled}
 * (env NORD_SECURITY_ENABLED). Com {@code false} a API fica aberta como antes, o que permite
 * publicar o backend antes do frontend com tela de login.
 */
@Component
public class SecurityProperties {

    private final boolean enabled;
    private final String jwtSecret;
    private final long inactivityMinutes;
    private final boolean allowEphemeralSecret;

    public SecurityProperties(
            @Value("${nord-tool.security.enabled:false}") boolean enabled,
            @Value("${nord-tool.security.jwt-secret:}") String jwtSecret,
            @Value("${nord-tool.security.inactivity-minutes:30}") long inactivityMinutes,
            @Value("${nord-tool.security.allow-ephemeral-secret:false}") boolean allowEphemeralSecret) {
        this.enabled = enabled;
        this.jwtSecret = jwtSecret;
        this.inactivityMinutes = inactivityMinutes;
        this.allowEphemeralSecret = allowEphemeralSecret;
    }

    public boolean isEnabled() { return enabled; }
    public String getJwtSecret() { return jwtSecret; }
    public long getInactivityMinutes() { return inactivityMinutes; }
    public boolean isAllowEphemeralSecret() { return allowEphemeralSecret; }
}
