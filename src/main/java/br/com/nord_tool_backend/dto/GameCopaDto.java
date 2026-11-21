package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class GameCopaDto {
    private Long id;
    private Integer nrRodadas;
    private Integer nrColocacao;
    private String txResultado;
    private java.time.LocalDateTime dhCriacao;
}
