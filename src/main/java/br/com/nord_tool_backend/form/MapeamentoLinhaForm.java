package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class MapeamentoLinhaForm {
    @Size(max = 80, message = "O grupo deve ter no máximo 80 caracteres")
    private String nmGrupo;

    @NotBlank(message = "Informe o item")
    @Size(max = 150, message = "O item deve ter no máximo 150 caracteres")
    private String nmItem;

    @Size(max = 10, message = "A unidade deve ter no máximo 10 caracteres")
    private String nmUnidade;

    @DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    @Digits(integer = 12, fraction = 3, message = "Quantidade inválida (até 12 inteiros e 3 decimais)")
    private BigDecimal qtProjeto;

    @DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    @Digits(integer = 12, fraction = 3, message = "Quantidade inválida (até 12 inteiros e 3 decimais)")
    private BigDecimal qtMedida;

    @DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    @Digits(integer = 12, fraction = 3, message = "Quantidade inválida (até 12 inteiros e 3 decimais)")
    private BigDecimal qtSolicitada;

    @Size(max = 500, message = "A observação da linha deve ter no máximo 500 caracteres")
    private String txObservacao;
}
