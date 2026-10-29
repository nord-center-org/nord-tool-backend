package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Tipologia de apartamento: nome e imagem da planta (data URL; null = manter, "" = remover). */
@Data
public class EscadinhaTipologiaForm {
    @NotBlank
    @Size(max = 60)
    private String nmTipologia;
    private String imagem;
}
