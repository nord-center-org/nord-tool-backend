package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class GameSalaForm {
    @NotNull(message = "Informe o jogo")
    @Pattern(regexp = "^[a-z0-9-]{1,40}$", message = "Jogo inválido")
    private String jogo;
}
