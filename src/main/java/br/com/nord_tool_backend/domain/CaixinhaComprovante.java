package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CaixinhaComprovante extends GlobalDomain {
    private Long idLancamento;
    private Long idArquivo;
    /** Vêm do join com arquivo_armazenado (sem os bytes). */
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    private LocalDateTime dhCriacao;
}
