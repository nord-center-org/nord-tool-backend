package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CronogramaSemanalDto {
    private Long id;
    private Integer idDiaSemana;
    private String nmDiaSemana;
    private String nmCronogramaSemanal;
    private String nmHorario;
    private String nmCategoria;
    private String nmStatusCronograma;
    private String txObservacao;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtPrazo;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtFinalizacao;

    public static CronogramaSemanalDto converterToDto(CronogramaSemanal cronogramaSemanal ) {
        return CronogramaSemanalDto.builder()
                .id(cronogramaSemanal.getId())
                .idDiaSemana(cronogramaSemanal.getIdDiaSemana())
                .nmDiaSemana(cronogramaSemanal.getNmDiaSemana())
                .nmCronogramaSemanal(cronogramaSemanal.getNmCronogramaSemanal())
                .nmHorario(cronogramaSemanal.getNmHorario())
                .nmCategoria(cronogramaSemanal.getNmCategoria())
                .nmStatusCronograma(cronogramaSemanal.getNmStatusCronograma())
                .txObservacao(cronogramaSemanal.getTxObservacao())
                .dtPrazo(cronogramaSemanal.getDtPrazo())
                .dtFinalizacao(cronogramaSemanal.getDtFinalizacao())
                .build();
    }
}
