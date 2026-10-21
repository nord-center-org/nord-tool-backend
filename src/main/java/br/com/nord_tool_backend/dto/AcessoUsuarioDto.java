package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data @AllArgsConstructor @NoArgsConstructor
public class AcessoUsuarioDto {
    private Long id;
    private String nmNome;
    private String nmEmail;
    private String cdPerfil;
    private boolean inAtivo;
    private LocalDateTime dhUltimoLogin;
    private String nrCelular;
    private Integer idEmpresa;
    private String nmEmpresa;
    private Integer idCargo;
    private String nmCargo;
    /** Nível efetivo por módulo (perfil + exceções do usuário); módulos sem acesso ficam fora. */
    private Map<String, String> acessos;
}
