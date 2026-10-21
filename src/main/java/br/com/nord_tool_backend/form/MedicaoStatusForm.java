package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MedicaoStatusForm {
    @NotBlank(message = "Informe o status")
    private String cdStatus;

    /** Versão que o cliente viu (divergência → 409). */
    private Integer nrVersao;
}
