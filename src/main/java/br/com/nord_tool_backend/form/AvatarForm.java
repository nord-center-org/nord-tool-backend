package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AvatarForm {
    @NotBlank(message = "Informe a configuração do avatar")
    @Size(max = 2000, message = "A configuração do avatar deve ter no máximo 2000 caracteres")
    private String dsConfig;
}
