package br.com.nord_tool_backend.domain.enums;

import java.util.Locale;

public enum StatusRequisicaoChaveEnum {
    ABERTO,
    RECEBIDO;

    public static StatusRequisicaoChaveEnum from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status da requisição de chave não informado");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
