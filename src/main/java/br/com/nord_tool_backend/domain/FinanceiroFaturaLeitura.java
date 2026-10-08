package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor parcial da fatura numa data: a evolução ao longo do ciclo. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroFaturaLeitura extends GlobalDomain {
    private Long idLancamento;
    private LocalDate dtLeitura;
    private BigDecimal vlLeitura;
}
