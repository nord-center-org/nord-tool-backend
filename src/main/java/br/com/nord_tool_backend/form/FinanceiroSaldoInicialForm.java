package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Digits;
import java.math.BigDecimal;

/** Saldo no começo do mês (o primeiro mês, ou uma correção). Vazio volta a usar o saldo final do mês anterior. */
@Data
public class FinanceiroSaldoInicialForm {
    @Digits(integer = 13, fraction = 2, message = "Valor inválido (até 13 inteiros e 2 decimais)")
    private BigDecimal vlSaldoInicial;
}
