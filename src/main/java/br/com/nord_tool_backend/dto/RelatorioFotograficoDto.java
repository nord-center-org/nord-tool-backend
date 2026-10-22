package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Relatório fotográfico. Na listagem vem sem itens; no detalhe, com itens e fotos. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatorioFotograficoDto {
    private Long id;
    private String nmTitulo;
    private String nmLocal;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dtRelatorio;
    private Integer nrFotosPagina;
    /** Layout da página no PDF (ver RelatorioLayouts). */
    private String cdLayout;
    /** RASCUNHO ou CONCLUIDO. */
    private String cdStatus;
    private Long idModelo;
    private String nmModelo;
    private Long idFase;
    private String nmFase;
    private Long idAutor;
    private String nmAutor;
    private Long qtFotos;
    private Integer nrVersao;
    private List<RelatorioItemDto> itens;
}
