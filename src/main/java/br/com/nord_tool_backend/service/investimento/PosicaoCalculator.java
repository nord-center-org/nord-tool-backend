package br.com.nord_tool_backend.service.investimento;

import br.com.nord_tool_backend.domain.FinanceiroOperacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Posição de um fundo a partir das operações: cotas, custo e preço médio (método do preço médio). */
public final class PosicaoCalculator {

    public static final String COMPRA = "COMPRA";
    public static final String VENDA = "VENDA";

    private PosicaoCalculator() {
    }

    public static final class Posicao {
        private final int cotas;
        private final BigDecimal custo;

        Posicao(int cotas, BigDecimal custo) {
            this.cotas = cotas;
            this.custo = custo;
        }

        public int getCotas() {
            return cotas;
        }

        /** Valor investido nas cotas que ainda estão na carteira. */
        public BigDecimal getCusto() {
            return custo;
        }

        public BigDecimal getPrecoMedio() {
            return cotas == 0 ? BigDecimal.ZERO : custo.divide(BigDecimal.valueOf(cotas), 2, RoundingMode.HALF_UP);
        }
    }

    /** No mesmo dia as compras vêm antes das vendas (dá para vender no dia o que comprou nele). */
    private static List<FinanceiroOperacao> ordenar(List<FinanceiroOperacao> operacoes) {
        List<FinanceiroOperacao> ordenadas = new ArrayList<>(operacoes);
        ordenadas.sort(Comparator.comparing(FinanceiroOperacao::getDtOperacao)
                .thenComparing(o -> COMPRA.equals(o.getCdTipo()) ? 0 : 1)
                .thenComparing(o -> o.getId() == null ? Long.MAX_VALUE : o.getId()));
        return ordenadas;
    }

    /** Posição final, ou null se em algum momento a venda ultrapassa as cotas que se tinha. */
    public static Posicao calcular(List<FinanceiroOperacao> operacoes) {
        int cotas = 0;
        BigDecimal custo = BigDecimal.ZERO;
        for (FinanceiroOperacao o : ordenar(operacoes)) {
            if (COMPRA.equals(o.getCdTipo())) {
                cotas += o.getQtCotas();
                custo = custo.add(o.getVlPreco().multiply(BigDecimal.valueOf(o.getQtCotas())));
            } else {
                if (o.getQtCotas() > cotas) return null;
                BigDecimal saida = custo.multiply(BigDecimal.valueOf(o.getQtCotas()))
                        .divide(BigDecimal.valueOf(cotas), 6, RoundingMode.HALF_UP);
                custo = custo.subtract(saida);
                cotas -= o.getQtCotas();
            }
        }
        return new Posicao(cotas, custo.setScale(2, RoundingMode.HALF_UP));
    }

    /** Cotas que a pessoa tinha ao fim do dia (inclusive). */
    public static int cotasEm(List<FinanceiroOperacao> operacoes, LocalDate data) {
        int cotas = 0;
        for (FinanceiroOperacao o : operacoes) {
            if (o.getDtOperacao().isAfter(data)) continue;
            cotas += COMPRA.equals(o.getCdTipo()) ? o.getQtCotas() : -o.getQtCotas();
        }
        return Math.max(cotas, 0);
    }
}
