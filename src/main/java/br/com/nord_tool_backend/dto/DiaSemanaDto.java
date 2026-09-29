package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.DiaSemana;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DiaSemanaDto{

    private Long idDiaSemana;
    private String nmDiaSemana;

    public static DiaSemanaDto converterToDto(DiaSemana diaSemana ) {
        return DiaSemanaDto.builder()
                .idDiaSemana(diaSemana.getId())
                .nmDiaSemana(diaSemana.getNmDiaSemana())
                .build();
    }
}
