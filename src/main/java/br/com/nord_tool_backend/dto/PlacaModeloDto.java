package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/** Modelo de placa: o formato (JSON do frontend) e se tem imagem de fundo e logo. */
@Data
public class PlacaModeloDto {
    private Long id;
    private String nmModelo;
    private JsonNode config;
    @JsonIgnore
    private String txConfig;
    @JsonIgnore
    private Long idArquivoFundo;
    @JsonIgnore
    private Long idArquivoLogo;
    private boolean inFundo;
    private boolean inLogo;
    /** Muda quando a imagem muda: o navegador não usa a antiga do cache. */
    private Long versaoFundo;
    private Long versaoLogo;
    private boolean inAtivo;
    private LocalDateTime dhAtualizacao;
}
