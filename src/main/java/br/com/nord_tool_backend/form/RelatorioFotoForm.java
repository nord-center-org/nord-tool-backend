package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class RelatorioFotoForm {
    @Size(max = 300, message = "A legenda deve ter no máximo 300 caracteres")
    private String txLegenda;

    @Min(value = 1, message = "A ordem deve ser maior que zero")
    private Integer nrOrdem;
}
