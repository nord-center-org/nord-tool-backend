package br.com.nord_tool_backend.domain.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;

public enum StatusFornecedorEnum {
    PESQUISANDO("Pesquisando"),
    ORCAMENTO("Orçamento"),
    CONTRATADO("Contratado");

    private final String rotulo;

    StatusFornecedorEnum(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Aceita o código (CONTRATADO) ou o rótulo (Contratado), sem diferenciar caixa/acentos. */
    public static Optional<StatusFornecedorEnum> de(String valor) {
        if (valor == null) return Optional.empty();
        String chave = normalizar(valor);
        return Arrays.stream(values())
                .filter(s -> normalizar(s.name()).equals(chave) || normalizar(s.rotulo).equals(chave))
                .findFirst();
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[\\s_-]+", "");
    }
}
