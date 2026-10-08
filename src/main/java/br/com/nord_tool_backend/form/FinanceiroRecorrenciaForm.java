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
public class FinanceiroRecorrenciaForm {
    @NotNull(message = "Informe a categoria")
    private Long idCategoria;

    @NotNull(message = "Informe de quem é")
    private Long idPessoa;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String dsRecorrencia;

    @NotNull(message = "Informe o valor")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @Digits(integer = 13, fraction = 2, message = "Valor inválido (até 13 inteiros e 2 decimais)")
    private BigDecimal vlRecorrencia;

    /** Dia do mês do lançamento gerado; vazio = dia 1 (mês curto usa o último dia). */
    @Min(value = 1, message = "O dia deve ser de 1 a 31")
    @Max(value = 31, message = "O dia deve ser de 1 a 31")
    private Integer nrDia;

    /** yyyy-MM-dd: primeiro mês em que a recorrência vale. */
    @NotNull(message = "Informe quando a recorrência começa")
    private LocalDate dtInicio;

    /** yyyy-MM-dd: último mês em que vale; vazio = sem fim. */
    private LocalDate dtFim;

    private Boolean inAtivo;

    /** Versão que o cliente viu; obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
