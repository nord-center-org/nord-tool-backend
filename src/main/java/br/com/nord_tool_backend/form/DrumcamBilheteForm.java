package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DrumcamBilheteForm {
    @NotNull(message = "Informe o papel")
    @Pattern(regexp = "^(camera|controle)$", message = "Papel inválido")
    private String papel;

    @Size(max = 60, message = "Nome grande demais")
    private String nome;
}
