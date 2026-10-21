package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AcessoUsuarioForm {
    @NotBlank(message = "Informe o perfil")
    private String cdPerfil;

    @NotNull(message = "Informe se o usuário está ativo")
    private Boolean inAtivo;

    /** Nível desejado por módulo (NENHUM, LEITURA, ESCRITA ou ADMIN). Módulo ausente = o que o perfil dá. */
    @NotNull(message = "Informe os acessos")
    private Map<String, String> acessos;
}
