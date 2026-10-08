package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Um mês do Financeiro (dt_competencia = dia 1): saldo inicial informado e fechamento. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroMes extends GlobalDomain {
    private LocalDate dtCompetencia;
    private BigDecimal vlSaldoInicial;
    private BigDecimal vlSaldoFinal;
    private Boolean inFechado;
    private LocalDateTime dhFechamento;
    private Integer nrVersao;
}
