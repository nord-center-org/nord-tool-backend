package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class TermoFotoDto {
    private Long idTermoFoto;
    private Long idTermoReprova;
    private Integer nrPagina;
    private Integer nrOrdem;
    private String txLegenda;
    private String dhAlteracao;
    /** Epoch millis da última alteração; use em ?v= para invalidar cache da imagem. */
    private Long nrVersao;
}
