package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class AcessosDto {
    private List<AcessoUsuarioDto> usuarios;
    private List<AcessoPerfilDto> perfis;
    /** Códigos dos módulos controlados, na ordem do enum. */
    private List<String> modulos;
    /** Acessos mínimos de cada cargo. */
    private List<AcessoCargoDto> cargos;
}
