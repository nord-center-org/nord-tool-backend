package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Imagem da prancha (data URL) usada no fundo da planta 2D. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscadinhaPlantaDto {
    private String planta;
}
