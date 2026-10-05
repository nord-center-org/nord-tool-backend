package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginForm {
    @NotBlank(message = "Informe o e-mail")
    private String email;

    @NotBlank(message = "Informe a senha")
    private String senha;
}
