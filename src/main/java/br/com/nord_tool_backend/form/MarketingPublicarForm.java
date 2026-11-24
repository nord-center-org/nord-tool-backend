package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/** Publicar o vídeo já enviado nas contas escolhidas. */
@Data
public class MarketingPublicarForm {
    @NotBlank(message = "Envie o vídeo antes de publicar")
    @Size(max = 80, message = "Vídeo inválido")
    private String idMidia;
    @NotBlank(message = "Informe o título")
    @Size(max = 150, message = "Título com no máximo 150 caracteres")
    private String txTitulo;
    @Size(max = 2200, message = "Legenda com no máximo 2.200 caracteres")
    private String txLegenda;
    private boolean inPublico = true;
    @NotEmpty(message = "Escolha pelo menos uma conta")
    @Size(max = 10, message = "No máximo 10 contas por vez")
    private List<Long> idsContas;
}
