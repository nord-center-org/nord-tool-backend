package br.com.nord_tool_backend.domain.enums;

import java.util.Optional;

/** Como o valor de uma categoria é estimado na projeção do mês. */
public enum TipoProjecaoEnum {
    /** Média dos meses anteriores, ou o valor real quando já lançado (salário, vale). */
    FIXA_MEDIA,
    /** Saldo final do mês anterior; calculado, nunca lançado à mão. */
    SALDO_ANTERIOR,
    /** Valor fixo cadastrado (apartamento, evolução de obra, investimentos). */
    FIXA_VALOR,
    /** Fatura do cartão: projetada pelo ritmo de gasto. */
    RITMO_FATURA,
    /** Média dos meses anteriores (assinaturas, conta de luz...). */
    VARIAVEL_MEDIA,
    /** Somente o valor digitado. */
    MANUAL;

    public static Optional<TipoProjecaoEnum> de(String valor) {
        return EnumParser.de(values(), valor);
    }
}
