package br.com.nord_tool_backend.domain.enums;

import java.util.Optional;

/** Natureza de uma categoria do Financeiro: dinheiro que entra ou que sai. */
public enum TipoFluxoEnum {
    ENTRADA,
    SAIDA;

    public static Optional<TipoFluxoEnum> de(String valor) {
        return EnumParser.de(values(), valor);
    }
}
