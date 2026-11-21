package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class GameConquistaDto {
    private String cdConquista;
    private String nmConquista;
    private String txDescricao;
    private java.time.LocalDateTime dhCriacao;
}
