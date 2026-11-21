package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class GameTotaisDto {
    private long nrPartidas;
    private long nrVitorias;
    private long nrRecorde;
    private long nrJogos;
    private long nrVitoriasDificil;
    private long nrVitoriasOito;
}
