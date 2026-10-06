package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CasamentoConfiguracaoForm {
    @NotBlank(message = "Informe o nome do casal")
    @Size(max = 200, message = "O nome do casal deve ter no máximo 200 caracteres")
    private String casal;

    @NotBlank(message = "Informe a data do casamento")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "A data do casamento deve estar no formato yyyy-MM-dd")
    private String dataCasamento;
}
