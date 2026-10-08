package br.com.nord_tool_backend.service.investimento;

import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PosicaoCalculatorTest {

    private static FinanceiroOperacao op(long id, String data, String tipo, int cotas, String preco) {
        FinanceiroOperacao o = new FinanceiroOperacao();
        o.setId(id);
        o.setDtOperacao(LocalDate.parse(data));
        o.setCdTipo(tipo);
        o.setQtCotas(cotas);
        o.setVlPreco(new BigDecimal(preco));
        return o;
    }

    @Test
    void precoMedioDasCompras() {
        PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 10, "120.00")));
        assertEquals(20, p.getCotas());
        assertEquals(new BigDecimal("2200.00"), p.getCusto());
        assertEquals(new BigDecimal("110.00"), p.getPrecoMedio());
    }

    @Test
    void vendaMantemOPrecoMedio() {
        PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 10, "120.00"),
                op(3, "2026-03-10", "VENDA", 5, "130.00")));
        assertEquals(15, p.getCotas());
        assertEquals(new BigDecimal("110.00"), p.getPrecoMedio());
        assertEquals(new BigDecimal("1650.00"), p.getCusto());
    }

    @Test
    void zeraAoVenderTudo() {
        PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                op(1, "2026-01-10", "COMPRA", 3, "100.00"), op(2, "2026-01-11", "VENDA", 3, "90.00")));
        assertEquals(0, p.getCotas());
        assertEquals(BigDecimal.ZERO, p.getPrecoMedio());
    }

    @Test
    void vendaAcimaDasCotasInvalida() {
        assertNull(PosicaoCalculator.calcular(Arrays.asList(op(1, "2026-01-10", "COMPRA", 3, "100.00"), op(2, "2026-01-11", "VENDA", 4, "90.00"))));
        // a venda vem antes da compra na linha do tempo
        assertNull(PosicaoCalculator.calcular(Arrays.asList(op(1, "2026-02-10", "COMPRA", 5, "100.00"), op(2, "2026-01-11", "VENDA", 1, "90.00"))));
    }

    @Test
    void mesmoDiaCompraAntesDaVenda() {
        assertNotNull(PosicaoCalculator.calcular(Arrays.asList(op(2, "2026-01-10", "VENDA", 2, "90.00"), op(1, "2026-01-10", "COMPRA", 2, "100.00"))));
    }

    @Test
    void semOperacoes() {
        PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Collections.emptyList());
        assertEquals(0, p.getCotas());
        assertEquals(0, PosicaoCalculator.cotasEm(Collections.emptyList(), LocalDate.parse("2026-01-01")));
    }

    @Test
    void cotasNaDataComContamOperacoesAteOFimDoDia() {
        java.util.List<FinanceiroOperacao> ops = Arrays.asList(
                op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 5, "100.00"), op(3, "2026-02-20", "VENDA", 8, "100.00"));
        assertEquals(0, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-01-09")));
        assertEquals(10, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-01-10")));
        assertEquals(15, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-02-10")));
        assertEquals(7, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-02-28")));
    }
}
