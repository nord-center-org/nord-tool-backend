package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoLinhaDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceiroProjecaoServiceImplTest {

    private static final LocalDate OUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate SET = LocalDate.of(2026, 9, 1);
    private static final LocalDate AGO = LocalDate.of(2026, 8, 1);

    private FinanceiroRepository repository;
    private FinanceiroProjecaoRepository projecao;
    private FinanceiroProjecaoServiceImpl service;

    private FinanceiroCategoria salario, fatura, apto, luz, saldo;

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @BeforeEach
    void setUp() {
        repository = mock(FinanceiroRepository.class);
        projecao = mock(FinanceiroProjecaoRepository.class);
        // 08/10/2026 12:00 em São Paulo: o mês atual é outubro
        service = new FinanceiroProjecaoServiceImpl(repository, projecao, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("UTC")));

        salario = cat(1, "Salário", "ENTRADA", "FIXA_MEDIA", 1);
        saldo = cat(3, "Saldo anterior", "ENTRADA", "SALDO_ANTERIOR", 3);
        fatura = cat(4, "Fatura", "SAIDA", "RITMO_FATURA", 1);
        apto = cat(5, "Apartamento", "SAIDA", "FIXA_VALOR", 2);
        luz = cat(6, "Conta de luz", "SAIDA", "VARIAVEL_MEDIA", 3);
        when(repository.listarCategorias()).thenReturn(Arrays.asList(salario, saldo, fatura, apto, luz));
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("500.00", 3, 8)));
        when(projecao.listarRecorrencias()).thenReturn(Collections.emptyList());
        when(projecao.primeiraCompetencia()).thenReturn(Optional.empty());
        when(projecao.listarMeses(any(), any())).thenReturn(Collections.emptyList());
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Collections.emptyList());
        when(projecao.faturasDoMes(any(), any())).thenReturn(Collections.emptyList());
        when(projecao.buscarMes(any())).thenReturn(Optional.empty());
    }

    // ---------- montagem dos dados ----------

    private static FinanceiroCategoria cat(long id, String nome, String tipo, String projecao, int ordem) {
        FinanceiroCategoria c = new FinanceiroCategoria();
        c.setId(id);
        c.setNmCategoria(nome);
        c.setCdTipo(tipo);
        c.setCdProjecao(projecao);
        c.setNrOrdem(ordem);
        c.setInAtivo(true);
        return c;
    }

    private static FinanceiroConfiguracao config(String meta, int meses, int diaFatura) {
        FinanceiroConfiguracao c = new FinanceiroConfiguracao();
        c.setVlMetaSaldo(v(meta));
        c.setNrMesesMedia(meses);
        c.setNrDiaConferencia(8);
        c.setNrDiaFechamentoFatura(diaFatura);
        return c;
    }

    private static FinanceiroSomaMes soma(LocalDate mes, FinanceiroCategoria c, String total, int qt) {
        FinanceiroSomaMes s = new FinanceiroSomaMes();
        s.setDtCompetencia(mes);
        s.setIdCategoria(c.getId());
        s.setVlTotal(v(total));
        s.setQtLancamentos(qt);
        return s;
    }

    private static FinanceiroMes mes(LocalDate competencia, boolean fechado, String saldoFinal, String saldoInicial) {
        FinanceiroMes m = new FinanceiroMes();
        m.setDtCompetencia(competencia);
        m.setInFechado(fechado);
        m.setVlSaldoFinal(saldoFinal == null ? null : v(saldoFinal));
        m.setVlSaldoInicial(saldoInicial == null ? null : v(saldoInicial));
        return m;
    }

    private void mesesNoBanco(FinanceiroMes... meses) {
        when(projecao.listarMeses(any(), any())).thenReturn(Arrays.asList(meses));
        for (FinanceiroMes m : meses) when(projecao.buscarMes(m.getDtCompetencia())).thenReturn(Optional.of(m));
    }

    private static FinanceiroRecorrencia recorrencia(long id, FinanceiroCategoria c, String valor, LocalDate inicio, LocalDate fim) {
        FinanceiroRecorrencia r = new FinanceiroRecorrencia();
        r.setId(id);
        r.setIdCategoria(c.getId());
        r.setIdPessoa(1L);
        r.setDsRecorrencia("Parcela");
        r.setVlRecorrencia(v(valor));
        r.setDtInicio(inicio);
        r.setDtFim(fim);
        r.setInAtivo(true);
        return r;
    }

    private static FinanceiroProjecaoLinhaDto linha(List<FinanceiroProjecaoLinhaDto> linhas, long idCategoria) {
        return linhas.stream().filter(l -> l.getIdCategoria() == idCategoria).findFirst().orElseThrow(AssertionError::new);
    }

    private void esperaErro(NordHttpEnum esperado, Runnable acao) {
        ValidacaoException ex = assertThrows(ValidacaoException.class, acao::run);
        assertEquals(esperado, ex.getHttpEnum());
    }

    /** Agosto e setembro fechados, outubro (atual) com salário e fatura lançados e o resto estimado. */
    private void cenarioDeOutubro() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, true, "1500.00", null));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, fatura, "2400.00", 1), soma(SET, fatura, "2400.00", 1), soma(SET, luz, "300.00", 1),
                soma(OUT, salario, "5000.00", 1), soma(OUT, fatura, "1200.00", 1)));
        FinanceiroFaturaAberta aberta = new FinanceiroFaturaAberta();
        aberta.setIdLancamento(70L);
        aberta.setIdCategoria(fatura.getId());
        aberta.setVlLancamento(v("1200.00"));
        aberta.setDtLeitura(LocalDate.of(2026, 9, 24));
        when(projecao.faturasDoMes(eq(OUT), any())).thenReturn(Collections.singletonList(aberta));
        when(projecao.listarRecorrencias()).thenReturn(Collections.singletonList(recorrencia(9, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));
        when(projecao.contarPrevistos(OUT)).thenReturn(3);
    }

    // ---------- a conta do mês ----------

    @Test
    void mesAtualSomaOLancadoAsEstimativasEPartedoSaldoDoMesAnteriorFechado() {
        cenarioDeOutubro();

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        assertEquals("2026-10", mes.getCompetencia());
        assertFalse(mes.isFechado());
        assertTrue(mes.isEstimado());
        assertTrue(mes.isComSaldoAnterior());
        assertEquals(v("1500.00"), mes.getSaldoAnterior());
        assertEquals(v("5000.00"), mes.getTotalEntradas());
        // fatura 1200 + (1 - 16/30) * 2400 = 2320; apartamento pela recorrência; luz pela média (300)
        assertEquals(v("2320.00"), linha(mes.getSaidas(), 4).getProjetado());
        assertEquals("RITMO", linha(mes.getSaidas(), 4).getOrigem());
        assertEquals(v("1956.42"), linha(mes.getSaidas(), 5).getProjetado());
        assertEquals("RECORRENCIA", linha(mes.getSaidas(), 5).getOrigem());
        assertEquals(v("300.00"), linha(mes.getSaidas(), 6).getProjetado());
        assertEquals(v("4576.42"), mes.getTotalSaidas());
        assertEquals(v("1923.58"), mes.getSaldoFinal());
        assertEquals(v("500.00"), mes.getMetaSaldo());
        assertEquals(v("1423.58"), mes.getFolga());
        assertEquals("VERDE", mes.getSituacao());
        assertEquals(3, mes.getQtPrevistos());
        // o saldo anterior não é uma linha: vem à parte
        assertEquals(1, mes.getEntradas().size());
        assertEquals(3, mes.getSaidas().size());
    }

    @Test
    void saldoFinalAbaixoDaMetaFicaVermelho() {
        cenarioDeOutubro();
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("3000.00", 3, 8)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        assertEquals("VERMELHO", mes.getSituacao());
        assertEquals(v("-1076.42"), mes.getFolga());
    }

    @Test
    void mesFechadoMostraOSaldoGravadoESemEstimativas() {
        cenarioDeOutubro();
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, true, "1500.00", null));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, salario, "3000.00", 1), soma(SET, salario, "3000.00", 1), soma(SET, apto, "1000.00", 1)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-09", null);

        assertTrue(mes.isFechado());
        assertFalse(mes.isEstimado());
        assertEquals(v("1500.00"), mes.getSaldoFinal());
        assertEquals(v("1000.00"), mes.getSaldoAnterior());
        assertEquals(0, mes.getQtPrevistos());
        assertEquals("real", "REAL".equals(linha(mes.getSaidas(), 5).getOrigem()) ? "real" : "outra");
    }

    @Test
    void mesesPassadosAindaAbertosContamSoOQueFoiLancadoEOSaldoInicialInformado() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(AGO, false, null, "100.00"));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, salario, "3000.00", 1), soma(AGO, apto, "1000.00", 1),
                soma(SET, salario, "3000.00", 1), soma(SET, luz, "200.00", 1)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        // agosto: 100 + 3000 - 1000 = 2100; setembro: 2100 + 3000 - 200 = 4900
        assertEquals(v("4900.00"), mes.getSaldoAnterior());
    }

    @Test
    void saldoInicialDoProprioMesPrevaleceSobreACadeia() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(SET));
        mesesNoBanco(mes(OUT, false, null, "777.00"));

        assertEquals(v("777.00"), service.obterMes("2026-10", null).getSaldoAnterior());
    }

    @Test
    void semNenhumDadoOSaldoAnteriorEZero() {
        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);
        assertEquals(v("0"), mes.getSaldoAnterior());
        assertEquals(v("0"), mes.getSaldoFinal());
        // sem nada, o saldo (zero) fica abaixo da meta de 500
        assertEquals("VERMELHO", mes.getSituacao());
        assertEquals(v("-500.00"), mes.getFolga());

        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("0.00", 3, 8)));
        assertEquals("VERDE", service.obterMes("2026-10", null).getSituacao());
    }

    @Test
    void comFiltroDePessoaNaoHaSaldoAnteriorNemMeta() {
        cenarioDeOutubro();

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", 2L);

        assertFalse(mes.isComSaldoAnterior());
        assertNull(mes.getSaldoAnterior());
        assertNull(mes.getMetaSaldo());
        assertNull(mes.getFolga());
        // a recorrência do apartamento é da pessoa 1: para a pessoa 2 o apartamento fica sem valor
        assertEquals(v("2380.00"), mes.getSaldoFinal());
        assertEquals("SEM_DADOS", linha(mes.getSaidas(), 5).getOrigem());
        assertEquals("VERDE", mes.getSituacao());
        verify(projecao, times(1)).somarPorCategoria(any(), any(), eq(2L));

        // para a pessoa 1 a recorrência entra
        assertEquals(v("423.58"), service.obterMes("2026-10", 1L).getSaldoFinal());
    }

    @Test
    void competenciaInvalidaEhRecusada() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.obterMes("10/2026", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.obterMes(null, null));
    }

    // ---------- fechar e reabrir ----------

    private void prepararFechamento() {
        cenarioDeOutubro();
        when(projecao.fecharMes(any(), any(), any())).thenReturn(1);
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(nick));
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(500L));
    }

    @Test
    void fecharGravaOSaldoRealSemEstimativasEGeraOsFixosDoMesSeguinte() {
        prepararFechamento();

        FinanceiroFechamentoDto r = service.fechar("2026-10", 9L);

        // real: saldo anterior 1500 + salário 5000 - fatura 1200 lançada (sem projetar o resto)
        verify(projecao).fecharMes(OUT, v("5300.00"), 9L);
        assertEquals(1, r.getRecorrenciasGeradas());
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository).inserirLancamento(captor.capture());
        FinanceiroLancamento gerado = captor.getValue();
        assertEquals(LocalDate.of(2026, 11, 1), gerado.getDtCompetencia());
        assertEquals(v("1956.42"), gerado.getVlLancamento());
        assertEquals(9L, gerado.getIdRecorrencia());
        assertEquals(9L, gerado.getIdUsuarioCriacao());
        assertEquals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(9L, YearMonth.of(2026, 11)), gerado.getCdRequisicao());
        assertFalse(gerado.getInRealizado());
    }

    @Test
    void fecharExigeOMesAnteriorFechadoQuandoHaDadosAntes() {
        cenarioDeOutubro();
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, false, null, null));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));
        verify(projecao, never()).fecharMes(any(), any(), any());
    }

    @Test
    void fecharPodeSerOPrimeiroMesOuTerSaldoInicial() {
        when(projecao.fecharMes(any(), any(), any())).thenReturn(1);
        // primeiro mês: nada antes
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(OUT));
        service.fechar("2026-10", 9L);

        // há dados antes, mas o mês tem saldo inicial informado
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(OUT, false, null, "250.00"));
        service.fechar("2026-10", 9L);

        verify(projecao, times(2)).fecharMes(eq(OUT), any(), eq(9L));
    }

    @Test
    void fecharMesJaFechadoOuQueFechaEmParaleloEhRecusado() {
        mesesNoBanco(mes(OUT, true, "1.00", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));

        when(projecao.buscarMes(OUT)).thenReturn(Optional.empty());
        when(projecao.fecharMes(any(), any(), any())).thenReturn(0);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));
    }

    @Test
    void reabrirSoOUltimoMesFechado() {
        mesesNoBanco(mes(SET, true, "1500.00", null));
        when(projecao.existeMesFechadoApos(SET)).thenReturn(true);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.reabrir("2026-09"));

        when(projecao.existeMesFechadoApos(SET)).thenReturn(false);
        when(projecao.reabrirMes(SET)).thenReturn(1);
        service.reabrir("2026-09");
        verify(projecao).reabrirMes(SET);

        esperaErro(NordHttpEnum.HTTP_400, () -> service.reabrir("2026-08"));
    }

    @Test
    void saldoInicialNaoMudaMesFechado() {
        mesesNoBanco(mes(SET, true, "1500.00", null));
        FinanceiroSaldoInicialForm f = new FinanceiroSaldoInicialForm();
        f.setVlSaldoInicial(v("10.00"));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.definirSaldoInicial("2026-09", f));

        when(projecao.definirSaldoInicial(OUT, v("10.00"))).thenReturn(1);
        service.definirSaldoInicial("2026-10", f);
        verify(projecao).garantirMes(OUT);
        verify(projecao).definirSaldoInicial(OUT, v("10.00"));
    }

    // ---------- recorrências ----------

    @Test
    void geracaoSoCriaOsLancamentosVigentesAtivosENaoContaOsQueJaExistiam() {
        FinanceiroCategoria inativa = cat(8, "Antiga", "SAIDA", "FIXA_VALOR", 8);
        inativa.setInAtivo(false);
        when(repository.listarCategorias()).thenReturn(Arrays.asList(apto, salario, inativa));
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(nick));
        FinanceiroRecorrencia vigente = recorrencia(1, apto, "100.00", LocalDate.of(2026, 1, 1), null);
        vigente.setNrDia(31);
        FinanceiroRecorrencia jaExiste = recorrencia(2, salario, "200.00", LocalDate.of(2026, 1, 1), null);
        FinanceiroRecorrencia acabou = recorrencia(3, apto, "300.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30));
        FinanceiroRecorrencia comecaDepois = recorrencia(4, apto, "400.00", LocalDate.of(2026, 12, 1), null);
        FinanceiroRecorrencia categoriaInativa = recorrencia(5, inativa, "500.00", LocalDate.of(2026, 1, 1), null);
        FinanceiroRecorrencia desligada = recorrencia(6, apto, "600.00", LocalDate.of(2026, 1, 1), null);
        desligada.setInAtivo(false);
        FinanceiroRecorrencia pessoaInativa = recorrencia(7, apto, "700.00", LocalDate.of(2026, 1, 1), null);
        pessoaInativa.setIdPessoa(99L);
        when(projecao.listarRecorrencias()).thenReturn(Arrays.asList(vigente, jaExiste, acabou, comecaDepois, categoriaInativa, desligada, pessoaInativa));
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        when(repository.inserirLancamento(captor.capture())).thenReturn(Optional.of(1L), Optional.empty());

        FinanceiroGeracaoDto r = service.gerarRecorrencias("2026-10", 9L);

        assertEquals("2026-10", r.getCompetencia());
        assertEquals(1, r.getCriados());
        assertEquals(2, captor.getAllValues().size());
        assertEquals(LocalDate.of(2026, 10, 31), captor.getAllValues().get(0).getDtLancamento());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getAllValues().get(1).getDtLancamento());
    }

    @Test
    void geracaoEmMesFechadoEhRecusada() {
        mesesNoBanco(mes(OUT, true, "1.00", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.gerarRecorrencias("2026-10", 9L));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void identificadorDaRecorrenciaEEstavelPorMes() {
        String a = FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 10));
        assertEquals(a, FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 10)));
        assertFalse(a.equals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 11))));
        assertFalse(a.equals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(2L, YearMonth.of(2026, 10))));
    }

    private FinanceiroRecorrenciaForm formRecorrencia() {
        FinanceiroRecorrenciaForm f = new FinanceiroRecorrenciaForm();
        f.setIdCategoria(apto.getId());
        f.setIdPessoa(1L);
        f.setVlRecorrencia(v("1956.42"));
        f.setDtInicio(LocalDate.of(2026, 1, 1));
        return f;
    }

    @Test
    void criaRecorrenciaValidandoCategoriaPessoaEPeriodo() {
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.buscarCategoria(5L)).thenReturn(Optional.of(apto));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(nick));
        when(projecao.inserirRecorrencia(any())).thenReturn(30L);
        when(projecao.buscarRecorrencia(30L)).thenReturn(Optional.of(recorrencia(30, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));

        assertEquals(30L, service.criarRecorrencia(formRecorrencia()).getIdRecorrencia());

        FinanceiroRecorrenciaForm fimAntes = formRecorrencia();
        fimAntes.setDtFim(LocalDate.of(2025, 12, 31));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(fimAntes));

        when(repository.buscarCategoria(3L)).thenReturn(Optional.of(saldo));
        FinanceiroRecorrenciaForm doSaldo = formRecorrencia();
        doSaldo.setIdCategoria(3L);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(doSaldo));

        nick.setInAtivo(false);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(formRecorrencia()));
    }

    @Test
    void atualizaRecorrenciaComControleDeVersao() {
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.buscarCategoria(5L)).thenReturn(Optional.of(apto));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(nick));
        when(projecao.buscarRecorrencia(30L)).thenReturn(Optional.of(recorrencia(30, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));
        FinanceiroRecorrenciaForm f = formRecorrencia();

        esperaErro(NordHttpEnum.HTTP_400, () -> service.atualizarRecorrencia(30L, f));

        f.setNrVersao(2);
        when(projecao.alterarRecorrencia(any(), eq(2))).thenReturn(0);
        esperaErro(NordHttpEnum.HTTP_409, () -> service.atualizarRecorrencia(30L, f));

        when(projecao.alterarRecorrencia(any(), eq(2))).thenReturn(1);
        assertEquals(30L, service.atualizarRecorrencia(30L, f).getIdRecorrencia());
    }

    // ---------- configuração ----------

    @Test
    void configuracaoAusenteEhCriadaComOsPadroes() {
        when(projecao.buscarConfiguracao()).thenReturn(Optional.empty(), Optional.of(config("0.00", 3, 8)));

        FinanceiroConfiguracaoDto c = service.obterConfiguracao();

        verify(projecao).garantirConfiguracao();
        assertEquals(3, c.getNrMesesMedia());
        assertEquals(8, c.getNrDiaFechamentoFatura());
    }

    @Test
    void atualizaAConfiguracao() {
        FinanceiroConfiguracaoForm f = new FinanceiroConfiguracaoForm();
        f.setVlMetaSaldo(v("500.00"));
        f.setNrMesesMedia(6);
        f.setNrDiaConferencia(10);
        f.setNrDiaFechamentoFatura(15);
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("500.00", 6, 15)));

        FinanceiroConfiguracaoDto c = service.atualizarConfiguracao(f);

        ArgumentCaptor<FinanceiroConfiguracao> captor = ArgumentCaptor.forClass(FinanceiroConfiguracao.class);
        verify(projecao).alterarConfiguracao(captor.capture());
        assertEquals(v("500.00"), captor.getValue().getVlMetaSaldo());
        assertEquals(6, captor.getValue().getNrMesesMedia());
        assertEquals(15, captor.getValue().getNrDiaFechamentoFatura());
        assertEquals(15, c.getNrDiaFechamentoFatura());
    }

    @Test
    void leiturasDeLancamentoInexistenteVira404() {
        when(repository.buscarLancamento(99L)).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_404, () -> service.listarLeituras(99L));
        verify(projecao, never()).listarLeituras(anyLong());
    }
}
