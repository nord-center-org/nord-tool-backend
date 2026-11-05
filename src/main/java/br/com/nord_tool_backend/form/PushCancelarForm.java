package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PushCancelarForm {
    @NotBlank(message = "Informe o aparelho")
    @Size(max = 1000, message = "Endereço de inscrição grande demais")
    private String endpoint;
}
