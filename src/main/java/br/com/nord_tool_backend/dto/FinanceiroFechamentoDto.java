package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de fechar um mês: a conta final e quantos lançamentos fixos do mês seguinte foram gerados. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroFechamentoDto {
    private FinanceiroProjecaoMesDto mes;
    private int recorrenciasGeradas;
}
