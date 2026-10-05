package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class TermoReprovaDto {
    private Long idTermoReprova;
    private Long idApartamentoVistoria;
    private Integer nrTermo;
    private String nmArquivo;
    private Integer nrPaginas;
    private String nmSituacao;
    private String txObservacao;
    private String dhCriacao;
    private String dhAlteracao;
    private Long nrVersao;
    private List<TermoFotoDto> fotos = new ArrayList<>();
    /** Avisos da operação (ex.: fotos removidas ao trocar o PDF por um com menos páginas). */
    private List<String> avisos = new ArrayList<>();
}
