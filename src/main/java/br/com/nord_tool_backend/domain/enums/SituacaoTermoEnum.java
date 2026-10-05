package br.com.nord_tool_backend.domain.enums;

import java.util.Arrays;
import java.util.Optional;

public enum SituacaoTermoEnum {
    PENDENTE, EM_ANDAMENTO, CONCLUIDO;

    public static Optional<SituacaoTermoEnum> de(String valor) {
        if (valor == null) return Optional.empty();
        return Arrays.stream(values()).filter(s -> s.name().equals(valor.trim().toUpperCase())).findFirst();
    }
}
