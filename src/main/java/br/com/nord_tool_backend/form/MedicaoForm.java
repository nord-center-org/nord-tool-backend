package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MedicaoForm {
    @NotNull(message = "Informe o número da medição")
    @Min(value = 1, message = "O número da medição deve ser maior que zero")
    private Integer nrMedicao;

    @NotBlank(message = "Informe o código do contrato")
    @Size(max = 20, message = "O código do contrato deve ter no máximo 20 caracteres")
    private String cdContrato;

    @NotBlank(message = "Informe a razão social")
    @Size(max = 150, message = "A razão social deve ter no máximo 150 caracteres")
    private String nmRazaoSocial;

    @NotBlank(message = "Informe o número do contrato")
    @Size(max = 40, message = "O número do contrato deve ter no máximo 40 caracteres")
    private String nrContrato;

    /** yyyy-MM-dd */
    @NotNull(message = "Informe a data da medição")
    private LocalDate dtMedicao;

    @NotNull(message = "Informe o valor da medição")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @Digits(integer = 12, fraction = 2, message = "Valor inválido (até 12 inteiros e 2 decimais)")
    private BigDecimal vlMedicao;

    @Size(max = 1000, message = "O memorial deve ter no máximo 1000 caracteres")
    private String txMemorial;

    /** Vazio na criação = PENDENTE. */
    private String cdStatus;

    /** Versão que o cliente viu; obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
