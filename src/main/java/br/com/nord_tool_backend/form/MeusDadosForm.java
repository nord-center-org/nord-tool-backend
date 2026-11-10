package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** O que a própria pessoa pode mudar nos seus dados (sem cargo, que dá acessos, e sem e-mail, que tem tela própria). */
@Data
public class MeusDadosForm {
    @NotBlank(message = "Informe o nome")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nmNome;

    @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Celular inválido")
    private String nrCelular;

    private Integer idEmpresa;
}
