package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Depois de registrar a partida: o recorde do jogo, se bateu o recorde e as conquistas novas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRegistroDto {
    private long nrRecorde;
    private boolean inNovoRecorde;
    private List<GameConquistaDto> novas;
}
