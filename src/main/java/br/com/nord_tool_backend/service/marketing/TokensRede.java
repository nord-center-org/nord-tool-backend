package br.com.nord_tool_backend.service.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Resultado de conectar (ou renovar) uma conta: tokens e quem é a conta na rede. */
@Getter
@AllArgsConstructor
public class TokensRede {
    private final String tokenAcesso;
    /** Nulo quando a rede não usa (ou não devolveu) token de renovação. */
    private final String tokenRenovacao;
    /** Quando o token de acesso vence; nulo se não vence. */
    private final LocalDateTime expiraEm;
    private final String idExterno;
    private final String nome;
    private final String foto;
}
