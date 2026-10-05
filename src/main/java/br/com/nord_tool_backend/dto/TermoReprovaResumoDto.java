package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class TermoReprovaResumoDto {
    private Long idTermoReprova;
    private Integer nrTermo;
    private String nmArquivo;
    private String nmSituacao;
    private Integer nrPaginas;
    private Integer qtFotos;
    /** dd/MM/yyyy HH:mm:ss */
    private String dhAlteracao;
    /** Epoch millis da última alteração; use em ?v= para invalidar cache. */
    private Long nrVersao;
}
