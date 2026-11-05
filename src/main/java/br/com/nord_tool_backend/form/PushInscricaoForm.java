package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** A inscrição que o navegador devolve (PushSubscription). */
@Data
public class PushInscricaoForm {
    @NotBlank(message = "Inscrição sem endereço")
    @Size(max = 1000, message = "Endereço de inscrição grande demais")
    @Pattern(regexp = "^https://.+", message = "Endereço de inscrição inválido")
    private String endpoint;
    @NotBlank(message = "Inscrição sem chave")
    @Size(max = 200, message = "Chave inválida")
    private String p256dh;
    @NotBlank(message = "Inscrição sem segredo")
    @Size(max = 100, message = "Segredo inválido")
    private String auth;
    @Size(max = 120, message = "Nome do aparelho grande demais")
    private String aparelho;
}
