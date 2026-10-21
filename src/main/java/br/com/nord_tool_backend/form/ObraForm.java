package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ObraForm {
    @NotBlank(message = "Informe o nome da obra")
    @Size(max = 120, message = "O nome da obra deve ter no máximo 120 caracteres")
    private String nmObra;

    @NotBlank(message = "Informe o código de acesso")
    @Pattern(regexp = "^[A-Za-z0-9-]{4,20}$", message = "O código deve ter de 4 a 20 letras, números ou hífen")
    private String cdAcesso;

    private Boolean inAtivo;

    /** Obrigatória na edição (divergência → 409). */
    private Integer nrVersao;
}
