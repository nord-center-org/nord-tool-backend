package br.com.nord_tool_backend.dto;

import lombok.Data;

/** Tipologia de apartamento da torre (a imagem vem à parte, só quando a vista do apartamento abre). */
@Data
public class EscadinhaTipologiaDto {
    private Long id;
    private Long idTorre;
    private String nmTipologia;
    private Boolean temImagem;
}
