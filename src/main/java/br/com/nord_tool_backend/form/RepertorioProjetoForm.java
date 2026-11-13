package br.com.nord_tool_backend.form;

import lombok.Data;

/** Uma criação do estúdio. Os limites são conferidos no serviço. */
@Data
public class RepertorioProjetoForm {
    private String nmTitulo;
    /** JSON (objeto) do projeto. */
    private String txDados;
}
