package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FinanceiroOperacaoForm {
    /** UUID gerado ao abrir o formulário: reenviar não duplica. */
    @NotBlank(message = "Informe o identificador da requisição")
    private String cdRequisicao;
    /** yyyy-MM-dd */
    @NotBlank(message = "Informe a data")
    private String dtOperacao;
    /** COMPRA | VENDA */
    @NotBlank(message = "Informe se é compra ou venda")
    private String cdTipo;
    @NotNull(message = "Informe a quantidade de cotas")
    private Integer qtCotas;
    @NotNull(message = "Informe o preço por cota")
    private BigDecimal vlPreco;
}
