package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoControleChavesDto {
    private Long idApartamentoVistoria;
    private String nmApartamentoVistoria;

    public static ApartamentoControleChavesDto converterToDto(ApartamentoVistoria apartamentoVistoria) {
        if (apartamentoVistoria == null) {
            return null;
        }

        return ApartamentoControleChavesDto.builder()
                .idApartamentoVistoria(apartamentoVistoria.getId())
                .nmApartamentoVistoria(apartamentoVistoria.getNmApartamentoVistoria())
                .build();
    }
}
