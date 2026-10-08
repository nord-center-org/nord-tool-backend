package br.com.nord_tool_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por módulo, usada em {@code @PreAuthorize("@acessoModulo.leitura(authentication, 'FINANCEIRO')")}.
 * As permissões do token têm o formato "MODULO:ACAO" (LEITURA | ESCRITA); "*" vale para todos os módulos e ESCRITA inclui LEITURA.
 * Com a segurança desligada (NORD_SECURITY_ENABLED=false) a API inteira fica aberta, então aqui também.
 */
@Component("acessoModulo")
public class AcessoModulo {

    private final SecurityProperties props;

    public AcessoModulo(SecurityProperties props) {
        this.props = props;
    }

    public boolean leitura(Authentication auth, String modulo) {
        return permitido(auth, modulo, false);
    }

    public boolean escrita(Authentication auth, String modulo) {
        return permitido(auth, modulo, true);
    }

    private boolean permitido(Authentication auth, String modulo, boolean exigeEscrita) {
        if (!props.isEnabled()) return true;
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado)) return false;
        for (String permissao : ((UsuarioAutenticado) auth.getPrincipal()).getPermissoes()) {
            int separador = permissao.indexOf(':');
            if (separador < 0) continue;
            String m = permissao.substring(0, separador).trim();
            String acao = permissao.substring(separador + 1).trim();
            if (!"*".equals(m) && !m.equalsIgnoreCase(modulo)) continue;
            if ("ESCRITA".equalsIgnoreCase(acao) || (!exigeEscrita && "LEITURA".equalsIgnoreCase(acao))) return true;
        }
        return false;
    }
}
