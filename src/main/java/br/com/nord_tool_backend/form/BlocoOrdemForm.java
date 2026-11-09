package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/** Todas as páginas do caderno na ordem nova. */
@Data
public class BlocoOrdemForm {
    @NotNull(message = "Informe a ordem das páginas")
    @Size(max = 500, message = "Páginas demais")
    private List<Long> ids;
}
