package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.utils.FormatDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class CronogramaSemanalForm {
    private Long idCronogramaSemanal;
    private Integer idDiaSemana;
    private String nmCronogramaSemanal;
    private String nmHorario;
    private String nmCategoria;
    private String nmStatusCronograma;
    private String txObservacao;
    @Schema(implementation = String.class, pattern = "^\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2}(:\\d{2})?)?$", example = "27/11/2026 09:00:00",
            description = "Data e hora em dd/MM/yyyy HH:mm:ss (também aceita dd/MM/yyyy e dd/MM/yyyy HH:mm)")
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtPrazo;
    @Schema(implementation = String.class, pattern = "^\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2}(:\\d{2})?)?$", example = "27/11/2026 09:00:00",
            description = "Data e hora em dd/MM/yyyy HH:mm:ss (também aceita dd/MM/yyyy e dd/MM/yyyy HH:mm)")
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtFinalizacao;
    private String nmTag;
    @Schema(implementation = String.class, pattern = "^\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2}(:\\d{2})?)?$", example = "27/11/2026 09:00:00",
            description = "Data e hora em dd/MM/yyyy HH:mm:ss (também aceita dd/MM/yyyy e dd/MM/yyyy HH:mm)")
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtAgendamento;
    /** v3: último dia da demanda pontual; precisa de dtAgendamento e não pode ser antes dele. */
    @Schema(implementation = String.class, pattern = "^\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2}(:\\d{2})?)?$", example = "27/11/2026 09:00:00",
            description = "Data e hora em dd/MM/yyyy HH:mm:ss (também aceita dd/MM/yyyy e dd/MM/yyyy HH:mm)")
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtFimAgendamento;
    private Boolean flFixo;

    public CronogramaSemanal converterToDomain() {
        return CronogramaSemanal.builder()
                .id(idCronogramaSemanal)
                .idDiaSemana(idDiaSemana)
                .nmCronogramaSemanal(nmCronogramaSemanal)
                .nmHorario(nmHorario)
                .nmCronogramaCategoria(nmCategoria)
                .nmStatusCronograma(nmStatusCronograma)
                .txObservacao(txObservacao)
                .dtPrazo(dtPrazo)
                .dtFinalizacao(dtFinalizacao)
                .nmTag(nmTag)
                .dtAgendamento(dtAgendamento)
                .dtFimAgendamento(dtFimAgendamento)
                .inCronogramaFixo(flFixo != null ? flFixo : Boolean.TRUE)
                .build();
    }

}
