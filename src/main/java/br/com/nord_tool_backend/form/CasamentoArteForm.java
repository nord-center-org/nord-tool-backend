package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Título e categoria de uma arte (para renomear ou trocar a categoria). */
@Data
public class CasamentoArteForm {
    @NotBlank(message = "Informe o título")
    @Size(max = 150, message = "Título com no máximo 150 caracteres")
    private String nmTitulo;
    @Size(max = 30, message = "Categoria inválida")
    private String cdCategoria;
}
