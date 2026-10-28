package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.IntegracaoToken;
import lombok.Data;

import java.time.LocalDateTime;

/** Link de integração listado na tela (sem a chave: ela só aparece ao criar). */
@Data
public class IntegracaoTokenDto {
    private Long idToken;
    private String cdEscopo;
    private String nmRotulo;
    private String txPrefixo;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhUltimoUso;
    /** Só na criação: a chave completa, mostrada uma única vez. */
    private String chave;

    public static IntegracaoTokenDto de(IntegracaoToken t) {
        IntegracaoTokenDto d = new IntegracaoTokenDto();
        d.setIdToken(t.getIdToken());
        d.setCdEscopo(t.getCdEscopo());
        d.setNmRotulo(t.getNmRotulo());
        d.setTxPrefixo(t.getTxPrefixo());
        d.setDhCriacao(t.getDhCriacao());
        d.setDhUltimoUso(t.getDhUltimoUso());
        return d;
    }
}
