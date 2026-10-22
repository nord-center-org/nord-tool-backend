package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CadastroForm {
    @NotBlank(message = "Informe o seu nome")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nmNome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 150, message = "E-mail inválido")
    private String nmEmail;

    @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Celular inválido")
    private String nrCelular;

    @NotBlank(message = "Informe o código da obra")
    @Size(max = 20, message = "Código da obra inválido")
    private String cdObra;

    @NotBlank(message = "Informe a senha")
    @Size(max = 200, message = "Senha inválida")
    private String senha;
}
