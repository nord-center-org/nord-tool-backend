package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Linha de marketing_conta com os tokens ainda cifrados (só dentro do backend; nunca vai para a API). */
@Data
public class MarketingContaRegistroDto {
    private Long id;
    private Long idUsuario;
    private String cdRede;
    private String txIdExterno;
    private String txNome;
    private String txFoto;
    private String txTokenAcesso;
    private String txTokenRenovacao;
    private LocalDateTime dhExpiraAcesso;
    private LocalDateTime dhCriacao;
}
