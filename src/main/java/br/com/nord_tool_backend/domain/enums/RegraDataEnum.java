package br.com.nord_tool_backend.domain.enums;

import java.util.Optional;

/** Quando uma entrada costuma cair: dia fixo do mês ou n-ésimo dia útil. */
public enum RegraDataEnum {
    DIA_MES,
    DIA_UTIL;

    public static Optional<RegraDataEnum> de(String valor) {
        return EnumParser.de(values(), valor);
    }
}
