package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PlacaForm {
    @NotNull(message = "Escolha o modelo")
    private Long idModelo;
    @NotBlank(message = "Dê um nome à placa")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
    private String nmTitulo;
    @NotNull(message = "Informe os textos da placa")
    private JsonNode valores;
}
