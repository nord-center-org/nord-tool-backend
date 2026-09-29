package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoriaDto {

    private Long idApartamentoVistoria;
    private String nmApartamentoVistoria;
    private Integer idDiaSemana;
    private String nmDiaSemana;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtApartamentoVigente;
    private String nmHorarioVistoria;
    private Integer idStatusVistoria;
    private String nmStatusVistoria;
    private boolean inMarcarRevistoria;
    private String txObservacaoRevistoria;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtRevistoriaVigente;

    public static ApartamentoVistoriaDto converterToDto(ApartamentoVistoria apartamentoVistoria ) {
        return ApartamentoVistoriaDto.builder()
                .idApartamentoVistoria(apartamentoVistoria.getId())
                .nmApartamentoVistoria(apartamentoVistoria.getNmApartamentoVistoria())
                .idDiaSemana(apartamentoVistoria.getIdDiaSemana())
                .nmDiaSemana(apartamentoVistoria.getNmDiaSemana())
                .dtApartamentoVigente(apartamentoVistoria.getDtApartamentoVigente())
                .nmHorarioVistoria(apartamentoVistoria.getNmHorarioVistoria())
                .idStatusVistoria(apartamentoVistoria.getIdStatusVistoria())
                .nmStatusVistoria(apartamentoVistoria.getNmStatusVistoria())
                .inMarcarRevistoria(apartamentoVistoria.isInMarcarRevistoria())
                .txObservacaoRevistoria(apartamentoVistoria.getTxObservacaoRevistoria())
                .dtRevistoriaVigente(apartamentoVistoria.getDtRevistoriaVigente())
                .build();
    }
}
