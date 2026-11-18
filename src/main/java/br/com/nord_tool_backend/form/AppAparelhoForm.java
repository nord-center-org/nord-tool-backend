package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AppAparelhoForm {
    @Size(max = 120, message = "Nome do aparelho muito longo")
    private String nmAparelho;
}
