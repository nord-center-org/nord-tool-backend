package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Modelos disponíveis, nível do usuário no módulo LUGIA e se o modo executar (agente) está liberado. */
@Data @AllArgsConstructor @NoArgsConstructor
public class LugiaModelosDto {
    private List<LugiaModeloDto> modelos;
    private String nivel;
    private boolean agenteDisponivel;
}
