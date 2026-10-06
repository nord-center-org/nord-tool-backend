package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class CasamentoFornecedorForm {
    @NotBlank(message = "Informe o nome do fornecedor")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nmFornecedor;

    @NotBlank(message = "Informe a categoria")
    @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres")
    private String nmCategoria;

    @Size(max = 150, message = "O contato deve ter no máximo 150 caracteres")
    private String txContato;

    /** PESQUISANDO | ORCAMENTO | CONTRATADO (também aceita o rótulo). Padrão: PESQUISANDO. */
    private String nmStatus;

    @DecimalMin(value = "0.00", message = "O valor não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Valor inválido (até 10 inteiros e 2 decimais)")
    private BigDecimal vlValor;

    @Size(max = 4000, message = "A observação deve ter no máximo 4000 caracteres")
    private String txObservacao;
}
