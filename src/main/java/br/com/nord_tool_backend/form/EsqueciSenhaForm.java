package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class EsqueciSenhaForm {
    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 150, message = "E-mail inválido")
    private String nmEmail;
}
