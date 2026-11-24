package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/** Começar a enviar um vídeo: nome, tamanho em bytes e tipo (video/mp4, video/quicktime, video/webm). */
@Data
public class MarketingMidiaForm {
    @NotBlank(message = "Informe o nome do vídeo")
    @Size(max = 200, message = "Nome muito longo")
    private String txNome;
    @Positive(message = "Vídeo vazio")
    private long nrTamanho;
    @NotBlank(message = "Informe o tipo do vídeo")
    @Size(max = 60, message = "Tipo inválido")
    private String txTipo;
}
