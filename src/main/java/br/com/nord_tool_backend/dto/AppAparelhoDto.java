package br.com.nord_tool_backend.dto;

import lombok.Data;

/** Aparelho com o app Android que pode renovar a sessão. */
@Data
public class AppAparelhoDto {
    private Long id;
    private Long idUsuario;
    private Integer nrVersaoSessao;
}
