package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SituacaoTermoForm {
    /** PENDENTE | EM_ANDAMENTO | CONCLUIDO */
    @NotBlank(message = "Informe a situação do termo")
    private String situacao;

    @Size(max = 4000, message = "A observação deve ter no máximo 4000 caracteres")
    private String observacao;
}
