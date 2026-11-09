package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class BlocoCadernoForm {
    @NotBlank(message = "Dê um nome ao caderno")
    @Size(max = 120, message = "O nome do caderno tem no máximo 120 caracteres")
    private String nmTitulo;
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor inválida")
    private String cdCor;
    /** Fundo da primeira página (só ao criar). */
    private String cdFundo;
    /** Obrigatória ao alterar. */
    private Integer nrVersao;
}
