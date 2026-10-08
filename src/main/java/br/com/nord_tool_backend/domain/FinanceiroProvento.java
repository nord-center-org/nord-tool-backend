package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Provento por cota: recebe quem tem cotas ao fim da data-com. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroProvento extends GlobalDomain {
    private Long idAtivo;
    private LocalDate dtCom;
    private LocalDate dtPagamento;
    private BigDecimal vlPorCota;
    /** MANUAL | COTACAO (importado do provedor) */
    private String cdOrigem;
}
