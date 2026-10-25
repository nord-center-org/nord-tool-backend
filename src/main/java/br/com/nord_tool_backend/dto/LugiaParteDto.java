package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Parte de uma mensagem da Lugia em formato neutro: TEXTO, IMAGEM ou PDF. */
@Data @AllArgsConstructor @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LugiaParteDto {
    public static final String TEXTO = "texto";
    public static final String IMAGEM = "imagem";
    public static final String PDF = "pdf";

    private String tipo;
    private String texto;
    private String mime;
    private String base64;
    private String nome;

    public static LugiaParteDto texto(String texto) {
        return new LugiaParteDto(TEXTO, texto, null, null, null);
    }
}
