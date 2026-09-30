package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.utils.FormatDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtPrazo;
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtFinalizacao;
    private String nmTag;
    @JsonDeserialize(using = FormatDateTimeDeserializer.class)
    private LocalDateTime dtAgendamento;
    private Boolean flFixo;

    public CronogramaSemanal converterToDomain() {
        return CronogramaSemanal.builder()
                .id(idCronogramaSemanal)
                .idDiaSemana(idDiaSemana)
                .nmCronogramaSemanal(nmCronogramaSemanal)
                .nmHorario(nmHorario)
                .nmCategoria(nmCategoria)
                .nmStatusCronograma(nmStatusCronograma)
                .txObservacao(txObservacao)
                .dtPrazo(dtPrazo)
                .dtFinalizacao(dtFinalizacao)
                .nmTag(nmTag)
                .dtAgendamento(dtAgendamento)
                .flFixo(flFixo != null ? flFixo : Boolean.TRUE)
                .build();
    }

}
