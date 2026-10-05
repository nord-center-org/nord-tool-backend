package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class LoginResponseDto {
    private String token;
    /** Instante de expiração do token (ISO-8601, UTC). */
    private String expiraEm;
    /** Minutos de inatividade tolerados antes de o token expirar. */
    private long inatividadeMinutos;
    private UsuarioDto usuario;
}
