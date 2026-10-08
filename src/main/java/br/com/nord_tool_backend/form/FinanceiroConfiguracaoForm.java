package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FinanceiroConfiguracaoForm {
    @NotNull(message = "Informe a meta de saldo")
    @DecimalMin(value = "0.00", message = "A meta de saldo não pode ser negativa")
    @Digits(integer = 13, fraction = 2, message = "Valor inválido (até 13 inteiros e 2 decimais)")
    private BigDecimal vlMetaSaldo;

    @NotNull(message = "Informe quantos meses entram na média")
    @Min(value = 1, message = "A média usa de 1 a 12 meses")
    @Max(value = 12, message = "A média usa de 1 a 12 meses")
    private Integer nrMesesMedia;

    @NotNull(message = "Informe o dia da conferência")
    @Min(value = 1, message = "O dia deve ser de 1 a 31")
    @Max(value = 31, message = "O dia deve ser de 1 a 31")
    private Integer nrDiaConferencia;

    @NotNull(message = "Informe o dia de fechamento da fatura")
    @Min(value = 1, message = "O dia deve ser de 1 a 31")
    @Max(value = 31, message = "O dia deve ser de 1 a 31")
    private Integer nrDiaFechamentoFatura;
}
