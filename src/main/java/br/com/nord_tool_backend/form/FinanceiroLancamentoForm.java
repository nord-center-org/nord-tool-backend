package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceiroLancamentoForm {
    /** UUID gerado pelo cliente ao abrir "Novo": reenviar o mesmo UUID não duplica. Obrigatório na criação. */
    private String cdRequisicao;

    /** yyyy-MM-dd: data do fato (vencimento ou recebimento). */
    @NotNull(message = "Informe a data")
    private LocalDate dtLancamento;

    /** Opcional (yyyy-MM-dd; só o mês vale). Sem ele, o lançamento pertence ao mês de dtLancamento. */
    private LocalDate dtCompetencia;

    @NotNull(message = "Informe a categoria")
    private Long idCategoria;

    @NotNull(message = "Informe de quem é o lançamento")
    private Long idPessoa;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String dsLancamento;

    /** Sempre positivo: entrada ou saída vem da categoria. Em lançamento parcelado é o valor de cada parcela. */
    @NotNull(message = "Informe o valor")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @Digits(integer = 13, fraction = 2, message = "Valor inválido (até 13 inteiros e 2 decimais)")
    private BigDecimal vlLancamento;

    private Boolean inRealizado;

    /** Só na criação: cria N lançamentos, um por mês a partir do mês informado (1 a 120; padrão 1). */
    @Min(value = 1, message = "O número de parcelas deve ser de 1 a 120")
    @Max(value = 120, message = "O número de parcelas deve ser de 1 a 120")
    private Integer qtParcelas;

    /** Versão que o cliente viu; obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
