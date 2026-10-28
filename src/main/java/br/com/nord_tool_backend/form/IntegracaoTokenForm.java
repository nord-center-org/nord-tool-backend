package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Novo link de integração: o rótulo diz para quem é (ex.: "Gemini"). */
@Data
public class IntegracaoTokenForm {
    @NotBlank(message = "Dê um nome ao link (ex.: Gemini)")
    @Size(max = 60, message = "O nome do link deve ter no máximo 60 caracteres")
    private String nmRotulo;
}
