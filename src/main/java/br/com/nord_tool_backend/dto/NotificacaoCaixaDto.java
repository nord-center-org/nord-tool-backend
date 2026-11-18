package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Avisos novos da caixa depois de um nrSeq e o maior nrSeq da pessoa (de onde continuar). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoCaixaDto {
    private long nrUltimo;
    private List<NotificacaoCaixaItemDto> itens;
}
