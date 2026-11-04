package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PlacaModeloForm {
    @NotBlank(message = "Dê um nome ao modelo")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
    private String nmModelo;
    @NotNull(message = "Informe o formato do modelo")
    private JsonNode config;
    private Boolean inAtivo;
}
