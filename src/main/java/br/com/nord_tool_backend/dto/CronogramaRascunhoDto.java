package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Demanda montada pela Lugia a partir do texto, ainda não salva. Datas no formato dos campos da tela:
 * {@code dtPrazo} AAAA-MM-DD e {@code dtAgendamento} AAAA-MM-DDTHH:mm. {@code diasSemana} usa os ids de dia_semana.
 */
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class CronogramaRascunhoDto {
    private String nmCronogramaSemanal;
    private String txObservacao;
    private String nmCategoria;
    private String nmStatusCronograma;
    private String nmTag;
    private String dtPrazo;
    private Boolean flFixo;
    private List<Integer> diasSemana;
    private String nmHorario;
    private String dtAgendamento;
}
