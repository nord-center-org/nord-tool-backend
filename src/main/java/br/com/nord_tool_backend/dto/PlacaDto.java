package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/** Placa de uma obra: o modelo e os textos dela (JSON do frontend). */
@Data
public class PlacaDto {
    private Long id;
    private Long idModelo;
    private String nmTitulo;
    private JsonNode valores;
    @JsonIgnore
    private String txValores;
    @JsonIgnore
    private Long idObra;
    private String nmUsuario;
    private LocalDateTime dhAtualizacao;
}
