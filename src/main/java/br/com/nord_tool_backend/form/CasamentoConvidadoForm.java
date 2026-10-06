package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CasamentoConvidadoForm {
    @NotBlank(message = "Informe o nome do convidado")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nmConvidado;

    @Size(max = 80, message = "O grupo deve ter no máximo 80 caracteres")
    private String nmGrupo;

    @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
    private String nrTelefone;

    @Size(max = 80, message = "A relação deve ter no máximo 80 caracteres")
    private String nmRelacao;

    /** NAO_CONVIDADO | CONVIDADO | CONFIRMADO | NAO_IRA (também aceita o rótulo). Padrão: NAO_CONVIDADO. */
    private String nmStatus;

    @Min(value = 0, message = "O número de acompanhantes não pode ser negativo")
    @Max(value = 50, message = "O número de acompanhantes deve ser no máximo 50")
    private Integer nrAcompanhantes;

    @Size(max = 40, message = "A mesa deve ter no máximo 40 caracteres")
    private String nmMesa;
}
