package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotNull;

/** Modelo de ciclos da obra (atividades padrão e duração do ciclo). */
@Data
public class EscadinhaModeloForm {
    @NotNull(message = "Informe o modelo de ciclos")
    private JsonNode modelo;
}
