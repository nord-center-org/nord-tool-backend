package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class UsuarioDto {
    private Long id;
    private String nome;
    private String email;
    private String perfil;
    private List<PerfilPermissao> permissoes;
}
