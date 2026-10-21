package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @AllArgsConstructor @NoArgsConstructor
public class AcessoPerfilDto {
    private String cdPerfil;
    private String nmPerfil;
    /** Nível que o perfil dá a cada módulo (sem exceções). */
    private Map<String, String> acessos;
}
