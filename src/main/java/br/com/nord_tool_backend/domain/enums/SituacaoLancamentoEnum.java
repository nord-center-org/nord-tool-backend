package br.com.nord_tool_backend.domain.enums;

import java.util.Optional;

/** Filtro do extrato: lançamentos já recebidos/pagos, ainda previstos, ou todos. */
public enum SituacaoLancamentoEnum {
    TODOS,
    REALIZADO,
    PREVISTO;

    public static Optional<SituacaoLancamentoEnum> de(String valor) {
        return EnumParser.de(values(), valor);
    }
}
