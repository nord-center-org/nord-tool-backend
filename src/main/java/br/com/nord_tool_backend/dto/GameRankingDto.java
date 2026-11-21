package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class GameRankingDto {
    private Long idUsuario;
    private String nmNome;
    private String dsAvatar;
    private long nrPartidas;
    private long nrVitorias;
    private long nrRecorde;
}
