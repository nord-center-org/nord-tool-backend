package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** O resumo de uma demanda do cronograma para montar um aviso. */
@Data
public class DemandaAvisoDto {
    private Long id;
    private String nmCronogramaSemanal;
    private Long idDono;
    private String nmStatusCronograma;
    private LocalDateTime dtAgendamento;
    private LocalDateTime dtPrazo;
    /** Dono e com quem foi compartilhada, separados por vírgula. */
    private String txEnvolvidos;
}
