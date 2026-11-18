package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AppAparelhoEntrarForm {
    @NotBlank(message = "Token do aparelho obrigatório")
    @Size(max = 100, message = "Token do aparelho inválido")
    private String txToken;
}
