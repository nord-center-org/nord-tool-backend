package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Totais dos lançamentos filtrados. O saldo aqui é entradas − saídas do filtro (sem saldo anterior). */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroResumoDto {
    private BigDecimal entradas;
    private BigDecimal saidas;
    private BigDecimal saldo;
    private BigDecimal entradasRealizadas;
    private BigDecimal saidasRealizadas;
    private Integer qtLancamentos;
    private Integer qtRealizados;
}
