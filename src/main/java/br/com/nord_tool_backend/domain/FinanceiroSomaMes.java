package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Soma dos lançamentos de uma categoria num mês. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroSomaMes {
    private LocalDate dtCompetencia;
    private Long idCategoria;
    private BigDecimal vlTotal;
    private Integer qtLancamentos;
}
