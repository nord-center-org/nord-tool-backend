package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TreinoSessaoExercicioDto {
    @JsonIgnore
    private Long idSessao;
    private Integer nrOrdem;
    private Long idExercicio;
    private String nmExercicio;
    private String txRepeticoes;
    private BigDecimal vlCarga;
    private Boolean inFeito;
}
