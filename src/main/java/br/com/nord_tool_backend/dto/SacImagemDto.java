package br.com.nord_tool_backend.dto;

import lombok.Data;

@Data
public class SacImagemDto {
    private Long id;
    private Long idChamado;
    private Long idArquivo;
    private Long idUsuario;
}
