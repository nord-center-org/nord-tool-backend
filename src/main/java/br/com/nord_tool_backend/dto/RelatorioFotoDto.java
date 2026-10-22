package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RelatorioFotoDto {
    private Long id;
    private Long idItem;
    private Integer nrOrdem;
    private String txLegenda;
    /** Muda quando a imagem é trocada (cortar/girar): serve para o navegador não usar a imagem antiga. */
    private Long idImagem;
    @JsonIgnore
    private Long idArquivo;
    @JsonIgnore
    private Long idRelatorio;
    @JsonIgnore
    private LocalDateTime dhCriacao;
}
