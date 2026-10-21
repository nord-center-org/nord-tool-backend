package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class FaseUsuariosForm {
    /** Lista completa de quem participa da fase (substitui a atual). */
    @NotNull(message = "Informe os usuários")
    private List<Long> idsUsuarios;
}
