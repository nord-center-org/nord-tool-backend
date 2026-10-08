package br.com.nord_tool_backend.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/** Converte texto no código do enum, sem diferenciar caixa nem espaços nas pontas. */
final class EnumParser {

    private EnumParser() {
    }

    static <E extends Enum<E>> Optional<E> de(E[] valores, String texto) {
        if (texto == null) return Optional.empty();
        String chave = texto.trim();
        return Arrays.stream(valores).filter(e -> e.name().equalsIgnoreCase(chave)).findFirst();
    }
}
