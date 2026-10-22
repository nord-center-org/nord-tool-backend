package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MapeamentoStatusForm {
    @NotBlank(message = "Informe o status")
    private String cdStatus;
    private Integer nrVersao;
}
