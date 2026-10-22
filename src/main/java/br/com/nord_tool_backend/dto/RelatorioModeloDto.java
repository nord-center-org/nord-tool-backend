package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Modelo de relatório fotográfico com os itens que ele pede. */
@Data
public class RelatorioModeloDto {
    private Long id;
    private String nmModelo;
    private String txDescricao;
    /** Fotos por página no PDF: 1, 2, 4 ou 6. */
    private Integer nrFotosPagina;
    private Boolean inAtivo;
    private Integer nrVersao;
    private List<RelatorioModeloItemDto> itens = new ArrayList<>();
}
