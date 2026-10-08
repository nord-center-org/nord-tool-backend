package br.com.nord_tool_backend.service.projecao;

import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Entrada;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Fatura;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Linha;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjecaoCalculatorTest {

    private static final YearMonth OUT = YearMonth.of(2026, 10);

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private static FinanceiroCategoria cat(long id, String nome, String tipo, String projecao, int ordem, boolean ativa) {
        FinanceiroCategoria c = new FinanceiroCategoria();
        c.setId(id);
        c.setNmCategoria(nome);
        c.setCdTipo(tipo);
        c.setCdProjecao(projecao);
        c.setNrOrdem(ordem);
        c.setInAtivo(ativa);
        return c;
    }

    private static final FinanceiroCategoria SALARIO = cat(1, "Salário", "ENTRADA", "FIXA_MEDIA", 1, true);
    private static final FinanceiroCategoria SALDO = cat(3, "Saldo anterior", "ENTRADA", "SALDO_ANTERIOR", 3, true);
    private static final FinanceiroCategoria FATURA = cat(4, "Fatura", "SAIDA", "RITMO_FATURA", 1, true);
    private static final FinanceiroCategoria APTO = cat(5, "Apartamento", "SAIDA", "FIXA_VALOR", 2, true);
    private static final FinanceiroCategoria LUZ = cat(6, "Conta de luz", "SAIDA", "VARIAVEL_MEDIA", 3, true);
    private static final FinanceiroCategoria JUAN = cat(7, "Juan", "SAIDA", "MANUAL", 4, true);

    /** Monta a entrada: {@code real} e {@code historico} são "id=valor" (mês anterior = índice 0). */
    private static class Montador {
        boolean estimar = true;
        int mesesNaMedia = 3;
        int diaFechamento = 8;
        List<FinanceiroCategoria> categorias = new ArrayList<>();
        Map<Long, BigDecimal> real = new HashMap<>();
        Map<Long, Integer> quantidade = new HashMap<>();
        Map<YearMonth, Map<Long, BigDecimal>> historico = new HashMap<>();
        Map<Long, BigDecimal> recorrencias = new HashMap<>();
        List<Fatura> faturas = new ArrayList<>();

        Montador categorias(FinanceiroCategoria... lista) { categorias.addAll(Arrays.asList(lista)); return this; }
        Montador lancado(FinanceiroCategoria c, String valor) { real.put(c.getId(), v(valor)); quantidade.put(c.getId(), 1); return this; }
        Montador mesAnterior(int mesesAtras, FinanceiroCategoria c, String valor) {
            historico.computeIfAbsent(OUT.minusMonths(mesesAtras), k -> new HashMap<>()).put(c.getId(), v(valor));
            return this;
        }
        Montador recorrencia(FinanceiroCategoria c, String valor) { recorrencias.put(c.getId(), v(valor)); return this; }
        Montador fatura(FinanceiroCategoria c, String valor, LocalDate leitura) { faturas.add(new Fatura(c.getId(), v(valor), leitura)); return this; }
        Resultado calcular() {
            return ProjecaoCalculator.calcular(new Entrada(OUT, estimar, categorias, real, quantidade, historico,
                    recorrencias, faturas, mesesNaMedia, diaFechamento));
        }
    }

    private static Linha linha(List<Linha> linhas, long idCategoria) {
        return linhas.stream().filter(l -> l.idCategoria == idCategoria).findFirst().orElseThrow(AssertionError::new);
    }

    // ---------- médias e valores lançados ----------

    @Test
    void salarioSemLancamentoUsaAMediaDosUltimosTresMeses() {
        Resultado r = new Montador().categorias(SALARIO)
                .mesAnterior(1, SALARIO, "3290.15").mesAnterior(2, SALARIO, "3952.06").mesAnterior(3, SALARIO, "3500.00").calcular();

        Linha l = linha(r.entradas, 1);
        assertEquals(v("3580.74"), l.projetado);
        assertEquals(BigDecimal.ZERO, l.real);
        assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, l.origem);
        assertEquals(v("3580.74"), r.totalEntradas);
    }

    @Test
    void valorLancadoNoMesPrevaleceSobreAMedia() {
        Resultado r = new Montador().categorias(SALARIO).lancado(SALARIO, "4000.00")
                .mesAnterior(1, SALARIO, "3000.00").calcular();

        assertEquals(v("4000.00"), linha(r.entradas, 1).projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_REAL, linha(r.entradas, 1).origem);
    }

    @Test
    void mediaConsideraSoMesesComLancamentoELimitaOsMesesPedidos() {
        Montador m = new Montador().categorias(LUZ).mesAnterior(1, LUZ, "300.00").mesAnterior(3, LUZ, "500.00");
        // o mês -2 não teve lançamento: média de 2 meses com dado
        assertEquals(v("400.00"), linha(m.calcular().saidas, 6).projetado);

        m.mesesNaMedia = 1;
        assertEquals(v("300.00"), linha(m.calcular().saidas, 6).projetado);

        m.mesesNaMedia = 12;
        m.mesAnterior(8, LUZ, "900.00");
        assertEquals(v("566.67"), linha(m.calcular().saidas, 6).projetado);
    }

    @Test
    void semHistoricoNemLancamentoFicaZeroSemDados() {
        Linha l = linha(new Montador().categorias(LUZ).calcular().saidas, 6);
        assertEquals(BigDecimal.ZERO, l.projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_SEM_DADOS, l.origem);
    }

    @Test
    void valorFixoPrefereLancamentoDepoisRecorrenciaDepoisMedia() {
        Montador m = new Montador().categorias(APTO).mesAnterior(1, APTO, "1900.00");
        assertEquals(v("1900.00"), linha(m.calcular().saidas, 5).projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, linha(m.calcular().saidas, 5).origem);

        m.recorrencia(APTO, "1956.42");
        assertEquals(v("1956.42"), linha(m.calcular().saidas, 5).projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_RECORRENCIA, linha(m.calcular().saidas, 5).origem);

        m.lancado(APTO, "2000.00");
        assertEquals(v("2000.00"), linha(m.calcular().saidas, 5).projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_REAL, linha(m.calcular().saidas, 5).origem);
    }

    @Test
    void categoriaManualSoValeOQueFoiLancado() {
        Montador m = new Montador().categorias(JUAN).mesAnterior(1, JUAN, "500.00");
        assertEquals(BigDecimal.ZERO, linha(m.calcular().saidas, 7).projetado);
        m.lancado(JUAN, "500.00");
        assertEquals(v("500.00"), linha(m.calcular().saidas, 7).projetado);
    }

    @Test
    void semEstimarSoValeOLancado() {
        Montador m = new Montador().categorias(SALARIO, LUZ, FATURA).lancado(LUZ, "355.23")
                .mesAnterior(1, SALARIO, "3000.00").mesAnterior(1, FATURA, "2000.00");
        m.estimar = false;

        Resultado r = m.calcular();

        assertEquals(BigDecimal.ZERO, linha(r.entradas, 1).projetado);
        assertEquals(v("355.23"), linha(r.saidas, 6).projetado);
        assertEquals(BigDecimal.ZERO, linha(r.saidas, 4).projetado);
        assertEquals(v("355.23"), r.totalSaidas);
    }

    // ---------- filtros de categoria, ordem e totais ----------

    @Test
    void saldoAnteriorSaiDasLinhasEInativaSemLancamentoSome() {
        FinanceiroCategoria inativa = cat(9, "Antiga", "SAIDA", "MANUAL", 9, false);
        FinanceiroCategoria inativaComLancamento = cat(10, "Antiga com uso", "SAIDA", "MANUAL", 10, false);
        Resultado r = new Montador().categorias(SALARIO, SALDO, inativa, inativaComLancamento).lancado(inativaComLancamento, "10.00").calcular();

        assertEquals(1, r.entradas.size());
        assertEquals(1, r.saidas.size());
        assertEquals(10L, (long) r.saidas.get(0).idCategoria);
    }

    @Test
    void linhasSeguemAOrdemDasCategoriasETotaisSeparamEntradaESaida() {
        Resultado r = new Montador().categorias(JUAN, LUZ, APTO, FATURA, SALARIO)
                .lancado(SALARIO, "5000.00").lancado(FATURA, "2380.38").lancado(APTO, "1956.42").lancado(LUZ, "355.23").lancado(JUAN, "500.00")
                .calcular();

        assertEquals(Arrays.asList(4L, 5L, 6L, 7L), ids(r.saidas));
        assertEquals(v("5000.00"), r.totalEntradas);
        assertEquals(v("5192.03"), r.totalSaidas);
    }

    private static List<Long> ids(List<Linha> linhas) {
        List<Long> ids = new ArrayList<>();
        for (Linha l : linhas) ids.add(l.idCategoria);
        return ids;
    }

    // ---------- fatura pelo ritmo ----------

    @Test
    void faturaComMediaSomaOParcialAoQueFaltaDoCiclo() {
        // fechamento dia 8: ciclo da fatura de outubro = 09/09 a 08/10 (30 dias); em 24/09 passaram 16
        BigDecimal r = ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 24), OUT, 8, v("2400.00"));
        assertEquals(v("2320.00"), r); // 1200 + (1 - 16/30) * 2400
    }

    @Test
    void faturaSemHistoricoUsaORitmoDoCiclo() {
        BigDecimal r = ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 24), OUT, 8, null);
        assertEquals(v("2250.00"), r); // 1200 * 30 / 16
    }

    @Test
    void faturaNuncaFicaAbaixoDoParcial() {
        BigDecimal r = ProjecaoCalculator.projetarFatura(v("3000.00"), LocalDate.of(2026, 9, 24), OUT, 8, v("2400.00"));
        assertEquals(v("4120.00"), r);
        assertTrue(r.compareTo(v("3000.00")) >= 0);
    }

    @Test
    void faturaSemLeituraCicloEncerradoOuLeituraForaDoCicloFicaNoParcial() {
        assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), null, OUT, 8, v("2400.00")));
        assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 10, 8), OUT, 8, v("2400.00")));
        assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 10, 20), OUT, 8, v("2400.00")));
        assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 1), OUT, 8, v("2400.00")));
    }

    @Test
    void primeiroDiaDoCicloPesaQuaseTudoNaMedia() {
        BigDecimal r = ProjecaoCalculator.projetarFatura(v("100.00"), LocalDate.of(2026, 9, 9), OUT, 8, v("3000.00"));
        assertEquals(v("3000.00"), r); // d = 1 de 30: 100 + 29/30 * 3000 = 3000
    }

    @Test
    void diaDeFechamentoMaiorQueOMesUsaOUltimoDia() {
        // fechamento "dia 31" em fevereiro/2027 vale 28/02; o ciclo começa em 01/02 (31/01 + 1)
        YearMonth fev = YearMonth.of(2027, 2);
        BigDecimal r = ProjecaoCalculator.projetarFatura(v("1000.00"), LocalDate.of(2027, 2, 14), fev, 31, null);
        assertEquals(v("2000.00"), r); // d = 14 de 28
    }

    @Test
    void linhaDeFaturaEmAbertoUsaOParcialMaisOQueFalta() {
        Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00")
                .mesAnterior(1, FATURA, "2400.00").mesAnterior(2, FATURA, "2400.00").mesAnterior(3, FATURA, "2400.00")
                .fatura(FATURA, "1200.00", LocalDate.of(2026, 9, 24));

        Linha l = linha(m.calcular().saidas, 4);

        assertEquals(v("2320.00"), l.projetado);
        assertEquals(v("1200.00"), l.real);
        assertEquals(ProjecaoCalculator.ORIGEM_RITMO, l.origem);
    }

    @Test
    void faturaSemLancamentoUsaAMediaEFaturaFechadaFicaNoValorLancado() {
        Montador m = new Montador().categorias(FATURA).mesAnterior(1, FATURA, "2000.00").mesAnterior(2, FATURA, "3000.00");
        assertEquals(v("2500.00"), linha(m.calcular().saidas, 4).projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, linha(m.calcular().saidas, 4).origem);

        Montador fechada = new Montador().categorias(FATURA).lancado(FATURA, "2380.38").mesAnterior(1, FATURA, "2400.00")
                .fatura(FATURA, "2380.38", LocalDate.of(2026, 10, 9));
        Linha l = linha(fechada.calcular().saidas, 4);
        assertEquals(v("2380.38"), l.projetado);
        assertEquals(ProjecaoCalculator.ORIGEM_REAL, l.origem);
    }

    @Test
    void faturaLancadaSemNenhumaFaturaAbertaFicaNoValorLancado() {
        Linha l = linha(new Montador().categorias(FATURA).lancado(FATURA, "900.00").mesAnterior(1, FATURA, "2400.00").calcular().saidas, 4);
        assertEquals(v("900.00"), l.projetado);
    }

    @Test
    void duasFaturasNaMesmaCategoriaDividemAMedia() {
        Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00")
                .mesAnterior(1, FATURA, "2400.00")
                .fatura(FATURA, "600.00", LocalDate.of(2026, 9, 24)).fatura(FATURA, "600.00", LocalDate.of(2026, 9, 24));

        assertEquals(v("2320.00"), linha(m.calcular().saidas, 4).projetado);
    }

    @Test
    void semEstimarAFaturaFicaNoValorLancado() {
        Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00").mesAnterior(1, FATURA, "2400.00")
                .fatura(FATURA, "1200.00", LocalDate.of(2026, 9, 24));
        m.estimar = false;
        assertEquals(v("1200.00"), linha(m.calcular().saidas, 4).projetado);
        assertEquals(Collections.emptyList(), new Montador().calcular().entradas);
    }
}
