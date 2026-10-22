package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @AllArgsConstructor @NoArgsConstructor
public class AcessoCargoDto {
    private Integer idCargo;
    private String nmCargo;
    /** Acessos mínimos que o cargo dá (módulo → nível). */
    private Map<String, String> acessos;
}
