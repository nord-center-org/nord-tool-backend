package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Conta de rede conectada (sem tokens). */
@Data
public class MarketingContaDto {
    private Long id;
    private String cdRede;
    private String txNome;
    private String txFoto;
    private LocalDateTime dhCriacao;
}
