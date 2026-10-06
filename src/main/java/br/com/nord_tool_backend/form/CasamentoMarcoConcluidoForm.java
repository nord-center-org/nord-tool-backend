package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CasamentoMarcoConcluidoForm {
    @NotNull(message = "Informe se o marco está concluído")
    private Boolean concluido;
}
