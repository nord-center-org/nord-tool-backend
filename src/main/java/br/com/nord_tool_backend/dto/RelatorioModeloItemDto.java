package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class RelatorioModeloItemDto {
    private Long id;
    @JsonIgnore
    private Long idModelo;
    private Integer nrOrdem;
    private String nmItem;
    private String txOrientacao;
}
