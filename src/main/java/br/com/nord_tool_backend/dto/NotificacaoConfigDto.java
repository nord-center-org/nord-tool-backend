package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** O que o navegador precisa para se inscrever: se está ligado e a chave pública VAPID do servidor. */
@Data
@AllArgsConstructor
public class NotificacaoConfigDto {
    private boolean habilitado;
    private String chavePublica;
}
