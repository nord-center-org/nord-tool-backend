package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Um vídeo/post da conta com as métricas da rede (nulo = a rede não informou). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingPostDto {
    private String id;
    private String txTitulo;
    private String txMiniatura;
    private String txLink;
    private String txData;
    private Long nrViews;
    private Long nrCurtidas;
    private Long nrComentarios;
}
