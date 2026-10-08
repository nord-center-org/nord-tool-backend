package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FinanceiroProventoForm {
    /** yyyy-MM-dd */
    @NotBlank(message = "Informe a data-com")
    private String dtCom;
    /** yyyy-MM-dd */
    @NotBlank(message = "Informe a data do pagamento")
    private String dtPagamento;
    @NotNull(message = "Informe o valor por cota")
    private BigDecimal vlPorCota;
}
