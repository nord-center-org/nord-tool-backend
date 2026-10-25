package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LugiaTituloForm {
    @NotBlank
    @Size(max = 200)
    private String titulo;
}
