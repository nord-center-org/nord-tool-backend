package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotNull;

/** O desenho da página (JSON do editor), o fundo e a versão que a tela tinha. */
@Data
public class BlocoConteudoForm {
    @NotNull(message = "Conteúdo da página ausente")
    private JsonNode conteudo;
    private String cdFundo;
    @NotNull(message = "Informe a versão (nrVersao)")
    private Integer nrVersao;
}
