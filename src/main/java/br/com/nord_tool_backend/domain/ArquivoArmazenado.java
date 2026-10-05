package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class ArquivoArmazenado extends GlobalDomain {
    private String nmProvedor;
    private String cdReferencia;
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    private String nmHashSha256;
    /** Só preenchido pela consulta com conteúdo e no provedor POSTGRES. */
    private byte[] binConteudo;
    private LocalDateTime dhCriacao;
}
