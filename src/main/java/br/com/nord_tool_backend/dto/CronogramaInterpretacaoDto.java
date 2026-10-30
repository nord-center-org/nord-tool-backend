package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Resposta da Lugia ao pedido em texto: uma frase para o chat e as demandas montadas. */
@Data @AllArgsConstructor @NoArgsConstructor
public class CronogramaInterpretacaoDto {
    private String mensagem;
    private List<CronogramaRascunhoDto> demandas;
}
