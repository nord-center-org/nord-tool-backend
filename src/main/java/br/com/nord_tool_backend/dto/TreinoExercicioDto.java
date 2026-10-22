package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TreinoExercicioDto {
    private Long id;
    @JsonIgnore
    private Long idTreino;
    private Integer nrOrdem;
    private String nmExercicio;
    private Integer nrSeries;
    private String txRepeticoes;
    private BigDecimal vlCarga;
    private Integer nrDescansoSeg;
    private String txObservacao;
}
