package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class GamePartidaDto {
    private Long id;
    private String cdJogo;
    private String cdModo;
    private Integer nrPontos;
    private Boolean inVitoria;
    private Integer nrColocacao;
    private Integer nrJogadores;
    private Integer nrBots;
    private String cdDificuldade;
    private Integer nrDuracaoS;
    private Long idCopa;
    private java.time.LocalDateTime dhCriacao;
}
