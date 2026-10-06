package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CasamentoAnexo extends GlobalDomain {
    private Long idFornecedor;
    private Long idArquivo;
    /** Vêm do join com arquivo_armazenado (sem os bytes). */
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    private String txDescricao;
    private LocalDateTime dhCriacao;
}
