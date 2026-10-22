package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CompartilhamentoForm {
    /** Lista completa: substitui o compartilhamento atual (vazia = deixa de compartilhar). */
    @NotNull(message = "Informe a lista de pessoas (idsUsuarios)")
    @Size(max = 200, message = "Compartilhe com no máximo 200 pessoas")
    private List<Long> idsUsuarios;
}
