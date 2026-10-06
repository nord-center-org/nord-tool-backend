package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class CasamentoMarcoForm {
    @NotBlank(message = "Informe o título do marco")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    private String nmTitulo;

    /** yyyy-MM-dd */
    private LocalDate dtPrazo;

    @Size(max = 4000, message = "A observação deve ter no máximo 4000 caracteres")
    private String txObservacao;
}
