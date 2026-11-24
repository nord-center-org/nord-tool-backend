package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Volta do login da rede: o código e o state que a rede devolveu ao frontend. */
@Data
public class MarketingConectarForm {
    @NotBlank(message = "Informe a rede")
    @Size(max = 20, message = "Rede inválida")
    private String cdRede;
    @NotBlank(message = "Faltou o código da rede")
    @Size(max = 2000, message = "Código inválido")
    private String txCodigo;
    @NotBlank(message = "Faltou a confirmação da rede")
    @Size(max = 500, message = "Confirmação inválida")
    private String txEstado;
}
