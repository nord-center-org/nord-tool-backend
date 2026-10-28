package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Novo status da demanda na esteira (pendentes, executar, aguardar, acompanhar, finalizada). */
@Data
public class StatusCronogramaForm {
    @NotBlank
    @Size(max = 20)
    private String nmStatusCronograma;
}
