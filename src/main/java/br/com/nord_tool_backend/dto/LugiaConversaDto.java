package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LugiaConversaDto {
    private Long id;
    private String titulo;
    private LocalDateTime atualizadaEm;
    /** Sessão do agente Claude ligada à conversa (modo executar). */
    @JsonIgnore
    private String sessaoAgente;
}
