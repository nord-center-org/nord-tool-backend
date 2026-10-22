package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/** Exercício da ficha. id ausente = exercício novo. */
@Data
public class TreinoExercicioForm {
    private Long id;

    @NotBlank(message = "Informe o exercício")
    @Size(max = 100, message = "O exercício deve ter no máximo 100 caracteres")
    private String nmExercicio;

    @Min(value = 1, message = "Séries: de 1 a 20")
    @Max(value = 20, message = "Séries: de 1 a 20")
    private Integer nrSeries;

    @Size(max = 20, message = "Repetições: no máximo 20 caracteres (ex.: 8-12)")
    private String txRepeticoes;

    @DecimalMin(value = "0", message = "A carga não pode ser negativa")
    @Digits(integer = 5, fraction = 2, message = "Carga inválida")
    private BigDecimal vlCarga;

    @Min(value = 0, message = "Descanso: de 0 a 900 segundos")
    @Max(value = 900, message = "Descanso: de 0 a 900 segundos")
    private Integer nrDescansoSeg;

    @Size(max = 300, message = "A observação do exercício deve ter no máximo 300 caracteres")
    private String txObservacao;
}
