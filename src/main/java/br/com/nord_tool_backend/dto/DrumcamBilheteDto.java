package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bilhete de uso único para abrir o WebSocket do Drumcam. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrumcamBilheteDto {
    private String bilhete;
    private String papel;
}
