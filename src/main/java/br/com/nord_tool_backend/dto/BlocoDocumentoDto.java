package br.com.nord_tool_backend.dto;

import lombok.Data;

/** v2: o documento de texto do caderno (estilo Word), em HTML já limpo pelo app. */
@Data
public class BlocoDocumentoDto {
    private String html;
    private Integer nrVersao;
}
