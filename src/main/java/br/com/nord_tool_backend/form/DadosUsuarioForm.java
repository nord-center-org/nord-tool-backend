package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DadosUsuarioForm {
    @NotBlank(message = "Informe o nome")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nmNome;

    @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Celular inválido")
    private String nrCelular;

    private Integer idEmpresa;
    private Integer idCargo;
}
