package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MapeamentoLinhaDto {
    private Long id;
    @JsonIgnore
    private Long idMapeamento;
    private Integer nrOrdem;
    private String nmGrupo;
    private String nmItem;
    private String nmUnidade;
    private BigDecimal qtProjeto;
    /** Na contagem, é o valor contado. */
    private BigDecimal qtMedida;
    private BigDecimal qtSolicitada;
    private String txObservacao;
}
