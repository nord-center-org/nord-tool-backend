package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Item respondido do relatório: orientação vinda do modelo, resposta (observação) e fotos. */
@Data
public class RelatorioItemDto {
    private Long id;
    private Long idRelatorio;
    private Integer nrOrdem;
    private String nmItem;
    private String txOrientacao;
    private String txResposta;
    private List<RelatorioFotoDto> fotos = new ArrayList<>();
}
