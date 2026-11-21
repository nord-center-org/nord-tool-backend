package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** O que a Central mostra de um jogo: os seus números, o ranking e as suas conquistas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResumoDto {
    private GameTotaisDto meus;
    private List<GameRankingDto> ranking;
    private List<GameConquistaDto> conquistas;
    private List<GamePartidaDto> historico;
}
