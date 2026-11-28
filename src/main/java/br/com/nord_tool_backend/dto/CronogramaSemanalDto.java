package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(implementation = String.class, example = "27/11/2026 09:00:00", description = "Data e hora em dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtPrazo;
    @Schema(implementation = String.class, example = "27/11/2026 09:00:00", description = "Data e hora em dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtFinalizacao;
    private String nmTag;
    @Schema(implementation = String.class, example = "27/11/2026 09:00:00", description = "Data e hora em dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtAgendamento;
    /** v3: último dia da demanda pontual (nulo = um dia só). */
    @Schema(implementation = String.class, example = "27/11/2026 09:00:00", description = "Data e hora em dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtFimAgendamento;
    private Boolean flFixo;
    private Long idDono;
    private String nmDono;
    /** false = compartilhado comigo por outra pessoa (só leitura). */
    private Boolean flProprio;
    private java.util.List<Long> idsCompartilhados;

    public static CronogramaSemanalDto converterToDto(CronogramaSemanal cronogramaSemanal ) {
        return CronogramaSemanalDto.builder()
                .id(cronogramaSemanal.getId())
                .idDiaSemana(cronogramaSemanal.getIdDiaSemana())
                .nmDiaSemana(cronogramaSemanal.getNmDiaSemana())
                .nmCronogramaSemanal(cronogramaSemanal.getNmCronogramaSemanal())
                .nmHorario(cronogramaSemanal.getNmHorario())
                .nmCategoria(cronogramaSemanal.getNmCronogramaCategoria())
                .nmStatusCronograma(cronogramaSemanal.getNmStatusCronograma())
                .txObservacao(cronogramaSemanal.getTxObservacao())
                .dtPrazo(cronogramaSemanal.getDtPrazo())
                .dtFinalizacao(cronogramaSemanal.getDtFinalizacao())
                .nmTag(cronogramaSemanal.getNmTag())
                .dtAgendamento(cronogramaSemanal.getDtAgendamento())
                .dtFimAgendamento(cronogramaSemanal.getDtFimAgendamento())
                .flFixo(cronogramaSemanal.getInCronogramaFixo())
                .idDono(cronogramaSemanal.getIdDono())
                .nmDono(cronogramaSemanal.getNmDono())
                .flProprio(!Boolean.FALSE.equals(cronogramaSemanal.getInProprio()))
                .idsCompartilhados(ids(cronogramaSemanal.getTxCompartilhados()))
                .build();
    }

    private static java.util.List<Long> ids(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new java.util.ArrayList<>();
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(x -> !x.isEmpty())
                .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
    }
}
