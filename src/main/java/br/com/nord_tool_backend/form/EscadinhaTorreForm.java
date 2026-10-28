package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Cria ou altera uma torre. planta: null mantém a imagem atual, "" remove, data URL troca.
 * Na alteração, nrVersao é obrigatório (divergente → 409).
 */
@Data
public class EscadinhaTorreForm {
    @NotBlank(message = "Informe o nome da torre")
    @Size(max = 80, message = "O nome da torre deve ter no máximo 80 caracteres")
    private String nmTorre;
    private Integer nrOrdem;
    @NotNull(message = "Informe a configuração da torre")
    private JsonNode config;
    @NotNull(message = "Informe os ciclos da torre")
    private JsonNode ciclos;
    private String planta;
    private Integer nrVersao;
}
