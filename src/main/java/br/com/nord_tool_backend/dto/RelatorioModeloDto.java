package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Modelo de relatório fotográfico com os itens que ele pede. */
@Data
public class RelatorioModeloDto {
    private Long id;
    private String nmModelo;
    private String txDescricao;
    /** Quantidade de fotos por página do layout. */
    private Integer nrFotosPagina;
    /** Layout da página no PDF (ver RelatorioLayouts). */
    private String cdLayout;
    /** HELVETICA, TIMES ou COURIER. */
    private String cdFonte;
    /** Cabeçalho padrão (título, data, local, fase, autor) em todas as páginas. */
    private Boolean inCabecalho;
    /** Opacidade da marca d'água, em %. */
    private Integer nrOpacidadeFundo;
    @JsonIgnore
    private Long idArquivoFundo;
    @JsonIgnore
    private Long idArquivoIcone;

    @JsonProperty("inFundo")
    public boolean isInFundo() {
        return idArquivoFundo != null;
    }

    @JsonProperty("inIcone")
    public boolean isInIcone() {
        return idArquivoIcone != null;
    }
    private Boolean inAtivo;
    private Integer nrVersao;
    private List<RelatorioModeloItemDto> itens = new ArrayList<>();
}
