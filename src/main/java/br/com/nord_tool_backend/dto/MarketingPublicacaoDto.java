package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Uma publicação feita pelo NordTool numa conta: status (ENVIANDO, PUBLICADO, PRIVADO, ERRO), link e aviso. */
@Data
public class MarketingPublicacaoDto {
    private Long id;
    private Long idConta;
    private String cdRede;
    private String txConta;
    private String txTitulo;
    private Boolean inPublico;
    private String cdStatus;
    private String txLink;
    private String txAviso;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAtualizacao;
}
