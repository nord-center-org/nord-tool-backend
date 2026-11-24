package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vídeo sendo recebido em partes: o id e o tamanho de cada parte. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMidiaDto {
    private String id;
    private long nrTamanhoParte;
    private int nrPartes;
}
