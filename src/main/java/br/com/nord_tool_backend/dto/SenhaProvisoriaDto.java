package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Resultado do reset de senha: a provisória aparece só nesta resposta (no banco fica apenas o hash). */
@Data @AllArgsConstructor @NoArgsConstructor
public class SenhaProvisoriaDto {
    private String senhaProvisoria;
    private LocalDateTime dhValidaAte;
    /** Também foi enviada por e-mail para a pessoa? */
    private boolean emailEnviado;
}
