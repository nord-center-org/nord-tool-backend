package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ingresso numa sala: o código, o jogo, a vaga (0 = quem hospeda) e o bilhete de uso único para abrir o WebSocket. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSalaDto {
    private String codigo;
    private String jogo;
    private int vaga;
    private boolean anfitriao;
    private String bilhete;
}
