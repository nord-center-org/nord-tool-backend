package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Troca do próprio e-mail: pede a senha atual para confirmar que é a pessoa mesmo. */
@Data
public class AlterarEmailForm {
    @NotBlank(message = "Informe o novo e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    private String nmEmail;

    @NotBlank(message = "Informe a senha atual")
    private String senhaAtual;
}
