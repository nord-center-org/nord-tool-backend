package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** Um aparelho em que a pessoa ativou as notificações. */
@Data
public class PushInscricaoDto {
    private Long id;
    @JsonIgnore
    private Long idUsuario;
    @JsonIgnore
    private String txEndpoint;
    @JsonIgnore
    private String txP256dh;
    @JsonIgnore
    private String txAuth;
    private String nmAparelho;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhUltimoEnvio;
}
