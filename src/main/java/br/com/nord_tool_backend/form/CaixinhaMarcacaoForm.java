package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** Marcar/desmarcar "lançado" e/ou "pago" direto na tabela. Campos ausentes não mudam. */
@Data
public class CaixinhaMarcacaoForm {
    private Boolean lancado;
    private Boolean pago;

    @NotNull(message = "Informe a versão do lançamento")
    private Integer nrVersao;
}
