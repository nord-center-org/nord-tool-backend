package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Primeiro login depois do reset pelo admin: e-mail, a senha provisória e a senha nova. */
@Data
public class DefinirSenhaForm {
    @NotBlank(message = "Informe o e-mail")
    @Size(max = 150, message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Informe a senha provisória")
    @Size(max = 72, message = "Senha provisória inválida")
    private String senhaProvisoria;

    @NotBlank(message = "Informe a nova senha")
    @Size(min = 10, max = 72, message = "A nova senha deve ter entre 10 e 72 caracteres")
    private String novaSenha;
}
