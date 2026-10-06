package br.com.nord_tool_backend.domain.enums;

import java.util.Arrays;
import java.util.Optional;

public enum StatusConvidadoEnum {
    NAO_CONVIDADO("Não convidado"),
    CONVIDADO("Convidado"),
    CONFIRMADO("Confirmado"),
    NAO_IRA("Não irá");

    private final String rotulo;

    StatusConvidadoEnum(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Aceita o código (NAO_IRA) ou o rótulo (Não irá), sem diferenciar caixa/acentos. */
    public static Optional<StatusConvidadoEnum> de(String valor) {
        if (valor == null) return Optional.empty();
        String chave = StatusFornecedorEnum.normalizar(valor);
        return Arrays.stream(values())
                .filter(s -> StatusFornecedorEnum.normalizar(s.name()).equals(chave)
                        || StatusFornecedorEnum.normalizar(s.rotulo).equals(chave))
                .findFirst();
    }
}
