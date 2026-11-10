package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bloco de notas v4: um item do Bloco de notas da pessoa para o chat dos cadernos. No catálogo vem sem conteúdo; nos
 * trechos vem o conteúdo bruto (JSON da página, HTML do documento ou JSON da planilha), que o serviço transforma em texto.
 */
@Data
public class BlocoTrechoDto {
    /** CADERNO (catálogo), PAGINA, DOCUMENTO_CADERNO, PLANILHA ou DOCUMENTO. */
    private String cdTipo;
    /** Caderno ou arquivo (planilha/documento). */
    private Long id;
    private String nmTitulo;
    private Long idPagina;
    private Integer nrPagina;
    private Integer qtPaginas;
    private LocalDateTime dhAlteracao;
    private String conteudo;
}
