package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AlterarSenhaForm {
    @NotBlank(message = "Informe a senha atual")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha")
    @Size(min = 10, max = 72, message = "A nova senha deve ter entre 10 e 72 caracteres")
    private String novaSenha;
}
