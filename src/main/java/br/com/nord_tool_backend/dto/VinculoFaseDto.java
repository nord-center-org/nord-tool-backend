package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class VinculoFaseDto {
    private int qtLigados;
    /** Apartamentos que continuam sem fase (sigla desconhecida ou repetida). */
    private int qtSemFase;
}
