package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** v2: o HTML do documento e a versão que a tela tinha. */
@Data
public class BlocoDocumentoForm {
    @NotNull(message = "Documento ausente")
    private String html;
    @NotNull(message = "Informe a versão (nrVersao)")
    private Integer nrVersao;
}
