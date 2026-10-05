package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class OrdemFotoForm {
    @NotNull(message = "Informe a foto")
    private Long idTermoFoto;

    @NotNull(message = "Informe a ordem")
    @Min(value = 0, message = "A ordem não pode ser negativa")
    private Integer nrOrdem;
}
