package br.com.nord_tool_backend.security;

import java.io.Serializable;
import java.util.List;

/** Principal extraído do JWT. */
public class UsuarioAutenticado implements Serializable {
    private final Long id;
    private final String email;
    private final String perfil;
    private final List<String> permissoes;

    public UsuarioAutenticado(Long id, String email, String perfil, List<String> permissoes) {
        this.id = id;
        this.email = email;
        this.perfil = perfil;
        this.permissoes = permissoes;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPerfil() { return perfil; }
    /** Formato "MODULO:ACAO", ex.: "*:ESCRITA". */
    public List<String> getPermissoes() { return permissoes; }

    @Override
    public String toString() { return email; }
}
