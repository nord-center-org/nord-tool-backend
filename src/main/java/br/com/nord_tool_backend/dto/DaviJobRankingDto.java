package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** Linha do ranking do minigame "Caça ao rato" (melhor pontuação de cada pessoa). */
@Data
public class DaviJobRankingDto {
    private Integer nrPosicao;
    private Long idUsuario;
    private String nmNome;
    private Integer nrPontos;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dhCriacao;
    private Boolean flProprio;
}
