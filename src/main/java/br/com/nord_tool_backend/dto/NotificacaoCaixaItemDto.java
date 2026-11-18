package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Um aviso da caixa de notificações. nrSeq cresce a cada aviso novo ou atualizado (avisos parecidos se juntam). */
@Data
public class NotificacaoCaixaItemDto {
    private Long id;
    private Long nrSeq;
    private String cdTipo;
    private String txTitulo;
    private String txCorpo;
    private String txUrl;
    private Integer nrQuantidade;
    private LocalDateTime dhAtualizacao;
}
