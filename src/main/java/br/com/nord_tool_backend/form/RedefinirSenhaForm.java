package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RedefinirSenhaForm {
    @NotBlank(message = "Link inválido")
    @Size(max = 100, message = "Link inválido")
    private String token;

    @NotBlank(message = "Informe a nova senha")
    @Size(max = 200, message = "Senha inválida")
    private String novaSenha;
}
