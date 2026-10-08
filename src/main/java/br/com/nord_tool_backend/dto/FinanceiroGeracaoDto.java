package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroGeracaoDto {
    /** yyyy-MM */
    private String competencia;
    /** Lançamentos criados agora (os que já existiam não contam). */
    private int criados;
}
