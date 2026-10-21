package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DecisaoCadastroForm {
    @NotNull(message = "Informe se o cadastro foi aprovado")
    private Boolean aprovar;

    /** Fases em que a pessoa entra ao ser aprovada (opcional). */
    private List<Long> idsFases;
}
