package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class TermoFoto extends GlobalDomain {
    private Long idTermoReprova;
    private Integer nrPagina;
    private Integer nrOrdem;
    private String txLegenda;
    private Long idArquivoImagem;
    private Long idArquivoMiniatura;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
}
