package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Resposta do administrador: novo status e, se quiser, um texto para quem abriu o chamado. */
@Data
public class SacRespostaForm {
    @NotBlank(message = "Informe o status")
    @Pattern(regexp = "ABERTO|EM_ANDAMENTO|RESOLVIDO|RECUSADO", message = "Status inválido")
    private String cdStatus;
    @Size(max = 4000, message = "A resposta deve ter no máximo 4000 caracteres")
    private String txResposta;
}
