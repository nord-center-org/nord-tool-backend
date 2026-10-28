package br.com.nord_tool_backend.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** Link de integração (chave de leitura) de um usuário; só o hash da chave fica no banco. */
@Data
public class IntegracaoToken {
    private Long idToken;
    private Long idUsuario;
    private String cdEscopo;
    private String nmRotulo;
    private String txPrefixo;
    private Boolean inAtivo;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhUltimoUso;
}
