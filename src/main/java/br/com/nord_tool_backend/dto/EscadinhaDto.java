package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Escadinha da obra escolhida: as torres e o modelo de ciclos (null = ainda não configurado). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscadinhaDto {
    private List<EscadinhaTorreDto> torres;
    private JsonNode modelo;
}
