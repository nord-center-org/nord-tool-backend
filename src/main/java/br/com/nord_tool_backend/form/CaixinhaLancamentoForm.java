package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CaixinhaLancamentoForm {
    /** UUID gerado pelo cliente ao abrir "Novo": reenviar o mesmo UUID não duplica o lançamento. Obrigatório na criação. */
    private String cdRequisicao;

    /** yyyy-MM-dd */
    @NotNull(message = "Informe a data")
    private LocalDate dtLancamento;

    @NotNull(message = "Informe o responsável")
    private Long idResponsavel;

    @NotBlank(message = "Informe o insumo")
    @Size(max = 500, message = "O insumo deve ter no máximo 500 caracteres")
    private String txInsumo;

    @NotNull(message = "Informe o valor")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "Valor inválido (até 10 inteiros e 2 decimais)")
    private BigDecimal vlValor;

    private Boolean inLancado;
    private Boolean inPago;

    /** Versão que o cliente viu; obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
