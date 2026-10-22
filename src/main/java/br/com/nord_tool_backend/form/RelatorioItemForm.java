package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Item de modelo ou de relatório. No relatório, id ausente = item novo. */
@Data
public class RelatorioItemForm {
    private Long id;

    @NotBlank(message = "Informe o nome do item")
    @Size(max = 150, message = "O item deve ter no máximo 150 caracteres")
    private String nmItem;

    @Size(max = 500, message = "A orientação deve ter no máximo 500 caracteres")
    private String txOrientacao;

    @Size(max = 2000, message = "A resposta deve ter no máximo 2000 caracteres")
    private String txResposta;
}
