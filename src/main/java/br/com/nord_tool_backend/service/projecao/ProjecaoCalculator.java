package br.com.nord_tool_backend.service.projecao;

import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.enums.TipoFluxoEnum;
import br.com.nord_tool_backend.domain.enums.TipoProjecaoEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Projeção de um mês do Financeiro, sem acesso a banco: recebe o que já foi lançado, o histórico dos meses
 * anteriores, as recorrências e as faturas em aberto, e devolve o valor real e o projetado de cada categoria.
 *
 * <p>Regras por tipo de projeção (só quando {@code estimar} é verdadeiro; senão vale apenas o que foi lançado):
 * <ul>
 *   <li>{@code FIXA_MEDIA} e {@code VARIAVEL_MEDIA}: o lançado no mês, ou a média dos meses anteriores que tiveram lançamento.</li>
 *   <li>{@code FIXA_VALOR}: o lançado no mês, ou o valor das recorrências vigentes, ou a média dos meses anteriores.</li>
 *   <li>{@code RITMO_FATURA}: ver {@link #projetarFatura}.</li>
 *   <li>{@code MANUAL}: só o lançado.</li>
 *   <li>{@code SALDO_ANTERIOR}: fora das linhas (é calculado à parte).</li>
 * </ul>
 */
public final class ProjecaoCalculator {

    public static final String ORIGEM_REAL = "REAL";
    public static final String ORIGEM_MEDIA = "MEDIA";
    public static final String ORIGEM_RECORRENCIA = "RECORRENCIA";
    public static final String ORIGEM_RITMO = "RITMO";
    public static final String ORIGEM_SEM_DADOS = "SEM_DADOS";

    private ProjecaoCalculator() {
    }

    /** Fatura do mês ainda aberta: o valor atual e a data da última leitura (nula se nunca foi atualizada). */
    public static final class Fatura {
        public final Long idCategoria;
        public final BigDecimal valor;
        public final LocalDate dtLeitura;

        public Fatura(Long idCategoria, BigDecimal valor, LocalDate dtLeitura) {
            this.idCategoria = idCategoria;
            this.valor = valor;
            this.dtLeitura = dtLeitura;
        }
    }

    public static final class Entrada {
        public final YearMonth competencia;
        public final boolean estimar;
        public final List<FinanceiroCategoria> categorias;
        /** Soma lançada no mês, por categoria. */
        public final Map<Long, BigDecimal> real;
        /** Quantos lançamentos existem no mês, por categoria. */
        public final Map<Long, Integer> quantidade;
        /** Soma por categoria em cada mês anterior; só entram categorias que tiveram lançamento. */
        public final Map<YearMonth, Map<Long, BigDecimal>> historico;
        /** Soma das recorrências vigentes no mês, por categoria. */
        public final Map<Long, BigDecimal> recorrencias;
        public final List<Fatura> faturas;
        public final int mesesNaMedia;
        public final int diaFechamentoFatura;

        public Entrada(YearMonth competencia, boolean estimar, List<FinanceiroCategoria> categorias,
                       Map<Long, BigDecimal> real, Map<Long, Integer> quantidade,
                       Map<YearMonth, Map<Long, BigDecimal>> historico, Map<Long, BigDecimal> recorrencias,
                       List<Fatura> faturas, int mesesNaMedia, int diaFechamentoFatura) {
            this.competencia = competencia;
            this.estimar = estimar;
            this.categorias = categorias;
            this.real = real;
            this.quantidade = quantidade;
            this.historico = historico;
            this.recorrencias = recorrencias;
            this.faturas = faturas;
            this.mesesNaMedia = mesesNaMedia;
            this.diaFechamentoFatura = diaFechamentoFatura;
        }
    }

    public static final class Linha {
        public final Long idCategoria;
        public final String nmCategoria;
        public final String cdTipo;
        public final String cdProjecao;
        public final BigDecimal real;
        public final BigDecimal projetado;
        public final String origem;
        public final String detalhe;

        Linha(FinanceiroCategoria c, BigDecimal real, BigDecimal projetado, String origem, String detalhe) {
            this.idCategoria = c.getId();
            this.nmCategoria = c.getNmCategoria();
            this.cdTipo = c.getCdTipo();
            this.cdProjecao = c.getCdProjecao();
            this.real = real;
            this.projetado = projetado;
            this.origem = origem;
            this.detalhe = detalhe;
        }
    }

    public static final class Resultado {
        public final List<Linha> entradas;
        public final List<Linha> saidas;
        public final BigDecimal totalEntradas;
        public final BigDecimal totalSaidas;

        Resultado(List<Linha> entradas, List<Linha> saidas) {
            this.entradas = entradas;
            this.saidas = saidas;
            this.totalEntradas = soma(entradas);
            this.totalSaidas = soma(saidas);
        }

        private static BigDecimal soma(List<Linha> linhas) {
            BigDecimal total = BigDecimal.ZERO;
            for (Linha l : linhas) total = total.add(l.projetado);
            return total;
        }
    }

    public static Resultado calcular(Entrada e) {
        List<FinanceiroCategoria> ordenadas = new ArrayList<>(e.categorias);
        ordenadas.sort(Comparator.comparing((FinanceiroCategoria c) -> c.getNrOrdem() == null ? 0 : c.getNrOrdem())
                .thenComparing(FinanceiroCategoria::getNmCategoria, String.CASE_INSENSITIVE_ORDER));

        List<Linha> entradas = new ArrayList<>();
        List<Linha> saidas = new ArrayList<>();
        for (FinanceiroCategoria c : ordenadas) {
            if (TipoProjecaoEnum.SALDO_ANTERIOR.name().equals(c.getCdProjecao())) continue;
            BigDecimal real = e.real.getOrDefault(c.getId(), BigDecimal.ZERO);
            boolean temLancamento = e.quantidade.getOrDefault(c.getId(), 0) > 0;
            // Categoria inativa e sem nada no mês não polui a tela.
            if (Boolean.FALSE.equals(c.getInAtivo()) && !temLancamento) continue;
            Linha linha = linha(e, c, real, temLancamento);
            (TipoFluxoEnum.ENTRADA.name().equals(c.getCdTipo()) ? entradas : saidas).add(linha);
        }
        return new Resultado(entradas, saidas);
    }

    private static Linha linha(Entrada e, FinanceiroCategoria c, BigDecimal real, boolean temLancamento) {
        TipoProjecaoEnum tipo = TipoProjecaoEnum.de(c.getCdProjecao()).orElse(TipoProjecaoEnum.MANUAL);
        BigDecimal media = e.estimar ? media(e, c.getId()) : null;
        int mesesMedia = e.estimar ? mesesComDado(e, c.getId()) : 0;

        if (tipo == TipoProjecaoEnum.RITMO_FATURA && temLancamento && e.estimar) {
            return fatura(e, c, real, media);
        }
        if (temLancamento || !e.estimar || tipo == TipoProjecaoEnum.MANUAL) {
            return new Linha(c, real, real, temLancamento ? ORIGEM_REAL : ORIGEM_SEM_DADOS, temLancamento ? "Valor lançado" : "Sem lançamentos");
        }
        if (tipo == TipoProjecaoEnum.FIXA_VALOR) {
            BigDecimal recorrencia = e.recorrencias.getOrDefault(c.getId(), BigDecimal.ZERO);
            if (recorrencia.signum() > 0) {
                return new Linha(c, real, recorrencia, ORIGEM_RECORRENCIA, "Valor fixo cadastrado");
            }
        }
        if (media != null) {
            return new Linha(c, real, media, ORIGEM_MEDIA, "Média dos últimos " + mesesMedia + " mês(es) com lançamento");
        }
        return new Linha(c, real, BigDecimal.ZERO, ORIGEM_SEM_DADOS, "Sem histórico para estimar");
    }

    private static Linha fatura(Entrada e, FinanceiroCategoria c, BigDecimal real, BigDecimal media) {
        List<Fatura> abertas = new ArrayList<>();
        for (Fatura f : e.faturas) if (c.getId().equals(f.idCategoria)) abertas.add(f);
        if (abertas.isEmpty()) {
            return new Linha(c, real, real, ORIGEM_REAL, "Valor lançado");
        }
        // Com mais de uma fatura na categoria (dois cartões), a média se divide entre elas.
        BigDecimal mediaPorFatura = media == null ? null
                : media.divide(BigDecimal.valueOf(abertas.size()), 2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO;
        boolean estimou = false;
        for (Fatura f : abertas) {
            BigDecimal projetada = projetarFatura(f.valor, f.dtLeitura, e.competencia, e.diaFechamentoFatura, mediaPorFatura);
            if (projetada.compareTo(f.valor) != 0) estimou = true;
            total = total.add(projetada);
        }
        String detalhe = estimou ? "Fatura em aberto: parcial atual + o que costuma faltar até o fechamento" : "Fatura já no valor final do ciclo";
        return new Linha(c, real, total, estimou ? ORIGEM_RITMO : ORIGEM_REAL, detalhe);
    }

    /**
     * Estima o valor final de uma fatura em aberto. O ciclo da fatura do mês M vai do dia seguinte ao
     * fechamento de M-1 até o fechamento de M. Com {@code d} dias já decorridos até a leitura e {@code D} dias no ciclo:
     * <ul>
     *   <li>com média histórica: {@code parcial + (1 − d/D) × média} (no começo vale a média; no fim, o parcial);</li>
     *   <li>sem histórico: {@code parcial × D/d} (o ritmo do ciclo);</li>
     *   <li>nunca abaixo do parcial; ciclo encerrado, leitura ausente ou fora do ciclo: o próprio parcial.</li>
     * </ul>
     */
    public static BigDecimal projetarFatura(BigDecimal parcial, LocalDate dtLeitura, YearMonth competencia,
                                            int diaFechamento, BigDecimal media) {
        if (dtLeitura == null) return parcial;
        LocalDate fim = dataDoDia(competencia, diaFechamento);
        LocalDate inicio = dataDoDia(competencia.minusMonths(1), diaFechamento).plusDays(1);
        if (!dtLeitura.isBefore(fim) || dtLeitura.isBefore(inicio)) return parcial;

        BigDecimal decorridos = BigDecimal.valueOf(ChronoUnit.DAYS.between(inicio, dtLeitura) + 1);
        BigDecimal total = BigDecimal.valueOf(ChronoUnit.DAYS.between(inicio, fim) + 1);
        BigDecimal projetada;
        if (media != null && media.signum() > 0) {
            BigDecimal faltante = BigDecimal.ONE.subtract(decorridos.divide(total, 10, RoundingMode.HALF_UP));
            projetada = parcial.add(faltante.multiply(media));
        } else {
            projetada = parcial.multiply(total).divide(decorridos, 2, RoundingMode.HALF_UP);
        }
        projetada = projetada.setScale(2, RoundingMode.HALF_UP);
        return projetada.max(parcial);
    }

    private static LocalDate dataDoDia(YearMonth mes, int dia) {
        return mes.atDay(Math.max(1, Math.min(dia, mes.lengthOfMonth())));
    }

    /** Média dos meses anteriores (até {@code mesesNaMedia}) em que a categoria teve lançamento; nulo se nenhum. */
    static BigDecimal media(Entrada e, Long idCategoria) {
        BigDecimal soma = BigDecimal.ZERO;
        int meses = 0;
        for (int i = 1; i <= e.mesesNaMedia; i++) {
            Map<Long, BigDecimal> mes = e.historico.get(e.competencia.minusMonths(i));
            if (mes != null && mes.containsKey(idCategoria)) {
                soma = soma.add(mes.get(idCategoria));
                meses++;
            }
        }
        return meses == 0 ? null : soma.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
    }

    private static int mesesComDado(Entrada e, Long idCategoria) {
        int meses = 0;
        for (int i = 1; i <= e.mesesNaMedia; i++) {
            Map<Long, BigDecimal> mes = e.historico.get(e.competencia.minusMonths(i));
            if (mes != null && mes.containsKey(idCategoria)) meses++;
        }
        return meses;
    }
}
