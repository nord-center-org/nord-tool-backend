package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Ferramenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FerramentaControleChavesDto {
    private Long idFerramenta;
    private String nmFerramenta;

    public static FerramentaControleChavesDto converterToDto(Ferramenta ferramenta) {
        if (ferramenta == null) {
            return null;
        }

        return FerramentaControleChavesDto.builder()
                .idFerramenta(ferramenta.getId())
                .nmFerramenta(ferramenta.getNmFerramenta())
                .build();
    }
}
