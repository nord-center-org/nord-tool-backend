package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.util.List;

/** Números de uma conta no painel: seguidores, views e os posts recentes; {@code txErro} quando a rede falhou. */
@Data
public class MarketingPainelDto {
    private Long idConta;
    private String cdRede;
    private String txConta;
    private String txFoto;
    private Long nrSeguidores;
    private Long nrViewsTotais;
    private List<MarketingPostDto> posts;
    private String txErro;
}
