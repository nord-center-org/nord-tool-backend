package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** A conta de um mês (o "fechamento" da planilha): entradas, saídas, saldo anterior e saldo final, reais ou projetados. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroProjecaoMesDto {
    /** yyyy-MM */
    private String competencia;
    private boolean fechado;
    /** Verdadeiro quando há valores estimados (mês aberto, atual ou futuro). */
    private boolean estimado;
    /** Falso quando o filtro é de uma pessoa: o saldo anterior é da conta toda e não se divide. */
    private boolean comSaldoAnterior;
    private BigDecimal saldoAnterior;
    private List<FinanceiroProjecaoLinhaDto> entradas;
    private List<FinanceiroProjecaoLinhaDto> saidas;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    /** Saldo anterior + entradas − saídas (mês fechado: o saldo gravado no fechamento). */
    private BigDecimal saldoFinal;
    /** Meta de saldo configurada (nula com filtro de pessoa). */
    private BigDecimal metaSaldo;
    /** Saldo final − meta: quanto ainda dá para gastar (ou quanto falta, se negativo). */
    private BigDecimal folga;
    /** VERDE se o saldo final atinge a meta; VERMELHO se não. */
    private String situacao;
    /** Lançamentos do mês ainda não marcados como recebidos/pagos. */
    private int qtPrevistos;
}
