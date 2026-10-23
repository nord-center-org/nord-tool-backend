package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class DaviJobPontuacaoForm {
    @NotNull(message = "Informe os pontos")
    @Min(value = 0, message = "Pontos inválidos")
    @Max(value = 10000, message = "Pontos inválidos")
    private Integer nrPontos;
}
