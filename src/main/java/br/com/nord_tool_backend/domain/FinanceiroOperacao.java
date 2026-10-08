package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Compra ou venda de cotas; vlPreco é o preço por cota. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroOperacao extends GlobalDomain {
    private String cdRequisicao;
    private Long idAtivo;
    private LocalDate dtOperacao;
    /** COMPRA | VENDA */
    private String cdTipo;
    private Integer qtCotas;
    private BigDecimal vlPreco;
}
