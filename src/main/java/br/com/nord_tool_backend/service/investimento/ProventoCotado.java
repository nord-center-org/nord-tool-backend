package br.com.nord_tool_backend.service.investimento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Provento informado pelo provedor de cotações. */
public final class ProventoCotado {
    private final LocalDate dtCom;
    private final LocalDate dtPagamento;
    private final BigDecimal valorPorCota;

    public ProventoCotado(LocalDate dtCom, LocalDate dtPagamento, BigDecimal valorPorCota) {
        this.dtCom = dtCom;
        this.dtPagamento = dtPagamento;
        this.valorPorCota = valorPorCota;
    }

    public LocalDate getDtCom() {
        return dtCom;
    }

    public LocalDate getDtPagamento() {
        return dtPagamento;
    }

    public BigDecimal getValorPorCota() {
        return valorPorCota;
    }
}
