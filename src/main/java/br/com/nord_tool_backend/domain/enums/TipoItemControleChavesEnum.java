package br.com.nord_tool_backend.domain.enums;

import java.util.Locale;

public enum TipoItemControleChavesEnum {
    APARTAMENTO,
    FERRAMENTA;

    public static TipoItemControleChavesEnum from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tipo do item da retirada não informado");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
