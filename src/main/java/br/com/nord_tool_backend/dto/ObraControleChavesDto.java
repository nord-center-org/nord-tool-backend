package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObraControleChavesDto {
    private String idObra;
    private String nmObra;

    public static ObraControleChavesDto converterToDto(String nmObra) {
        if (nmObra == null || nmObra.isBlank()) {
            return null;
        }

        return ObraControleChavesDto.builder()
                .idObra(nmObra)
                .nmObra(nmObra)
                .build();
    }
}
