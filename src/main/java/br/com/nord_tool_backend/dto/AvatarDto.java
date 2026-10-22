package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Avatar de um usuário; dsConfig nulo = ainda não montou (o frontend desenha o padrão). */
@Data
public class AvatarDto {
    private Long idUsuario;
    private String nmNome;
    private String nmCargo;
    private String dsConfig;
    private LocalDateTime dhAlteracao;
    /** true = é o próprio usuário logado. */
    private Boolean flProprio;
}
