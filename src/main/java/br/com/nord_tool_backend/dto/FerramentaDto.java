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
public class FerramentaDto {
    private Long id;
    private String nmFerramenta;
    private String nmCategoria;
    private String cdPatrimonio;
    private Boolean flAtivo;

    public static FerramentaDto converterToDto(Ferramenta ferramenta) {
        return FerramentaDto.builder()
                .id(ferramenta.getId())
                .nmFerramenta(ferramenta.getNmFerramenta())
                .nmCategoria(ferramenta.getNmCategoria())
                .cdPatrimonio(ferramenta.getCdPatrimonio())
                .flAtivo(ferramenta.getFlAtivo())
                .build();
    }
}
