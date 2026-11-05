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
    /** v2: o relatório é de quem está logado (quem recebeu compartilhado só vê). */
    private Boolean inProprio;
    /** v2: com quem está compartilhado ("3,7,9"); vai para a tela como idsCompartilhados. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String txCompartilhados;
    private List<RelatorioItemDto> itens;

    public List<Long> getIdsCompartilhados() {
        List<Long> ids = new java.util.ArrayList<>();
        if (txCompartilhados == null || txCompartilhados.isEmpty()) return ids;
        for (String t : txCompartilhados.split(",")) {
            if (!t.trim().isEmpty()) ids.add(Long.valueOf(t.trim()));
        }
        return ids;
    }
}
