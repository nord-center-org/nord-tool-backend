package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** Marca um lançamento como recebido/pago (ou volta para previsto). */
@Data
public class FinanceiroRealizadoForm {
    @NotNull(message = "Informe se o lançamento foi realizado")
    private Boolean inRealizado;

    @NotNull(message = "Informe a versão do lançamento (nrVersao)")
    private Integer nrVersao;
}
