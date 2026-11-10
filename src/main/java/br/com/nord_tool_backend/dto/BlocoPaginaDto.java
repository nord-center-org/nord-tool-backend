package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/** Página do caderno. O desenho (conteudo) só vem quando a página é aberta. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlocoPaginaDto {
    private Long id;
    private Long idCaderno;
    private Integer nrOrdem;
    /** BRANCO, PAUTADO, QUADRICULADO, PONTILHADO ou PDF (com imagem de fundo). */
    private String cdFundo;
    private Integer nrLargura;
    private Integer nrAltura;
    /** v2: 0, 90, 180 ou 270 (o fundo de PDF gira junto). */
    private Integer nrRotacao;
    /** v2: PDF original e a página dele (a tela desenha do PDF, na resolução do zoom). */
    private Long idPdf;
    private Integer nrPaginaPdf;
    private Boolean inFundo;
    @JsonIgnore
    private Long idArquivoFundo;
    private Integer nrVersao;
    private LocalDateTime dhAlteracao;
    private JsonNode conteudo;
}
