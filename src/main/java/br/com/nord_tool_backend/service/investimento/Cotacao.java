package br.com.nord_tool_backend.service.investimento;

import java.math.BigDecimal;
import java.time.Instant;

/** Preço de um fundo num momento. */
public final class Cotacao {
    private final BigDecimal preco;
    private final Instant momento;

    public Cotacao(BigDecimal preco, Instant momento) {
        this.preco = preco;
        this.momento = momento;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Instant getMomento() {
        return momento;
    }
}
