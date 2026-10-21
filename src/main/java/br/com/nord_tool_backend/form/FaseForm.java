package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class FaseForm {
    @NotBlank(message = "Informe o nome da fase")
    @Size(max = 80, message = "O nome da fase deve ter no máximo 80 caracteres")
    private String nmFase;

    /** Sigla curta (ex.: N1). Opcional. */
    @Pattern(regexp = "^$|^[A-Za-z0-9]{1,10}$", message = "A sigla deve ter até 10 letras ou números")
    private String cdFase;

    @Min(value = 0, message = "Ordem inválida")
    @Max(value = 999, message = "Ordem inválida")
    private Integer nrOrdem;

    private Boolean inAtivo;

    /** Obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
