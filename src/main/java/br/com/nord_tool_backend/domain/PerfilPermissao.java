package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @AllArgsConstructor @NoArgsConstructor
public class PerfilPermissao implements Serializable {
    /** "*" = todos os módulos. */
    private String cdModulo;
    /** LEITURA | ESCRITA. */
    private String cdAcao;
}
