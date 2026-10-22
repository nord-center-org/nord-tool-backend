package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AcessosCargoForm {
    /** Nível por módulo (NENHUM, LEITURA, ESCRITA ou ADMIN). Módulo ausente ou NENHUM = o cargo não dá. */
    @NotNull(message = "Informe os acessos")
    private Map<String, String> acessos;
}
