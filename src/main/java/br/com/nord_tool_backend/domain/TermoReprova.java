package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class TermoReprova extends GlobalDomain {
    private Long idApartamentoVistoria;
    private Integer nrTermo;
    private Long idArquivo;
    /** Vem do join com arquivo_armazenado (sem os bytes). */
    private String nmArquivo;
    private Integer nrPaginas;
    private String nmSituacao;
    private String txObservacao;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    /** Agregado, só nas consultas de leitura. */
    private Integer qtFotos;
}
