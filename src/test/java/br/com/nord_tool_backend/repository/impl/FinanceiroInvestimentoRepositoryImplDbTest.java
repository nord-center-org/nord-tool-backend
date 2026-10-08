package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroProvento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Queries de investimentos num PostgreSQL de TESTE (ver FinanceiroRepositoryImplDbTest). Só roda com NORD_TEST_DATABASE_URL. */
@EnabledIfEnvironmentVariable(named = "NORD_TEST_DATABASE_URL", matches = ".+")
class FinanceiroInvestimentoRepositoryImplDbTest {

    private NamedParameterJdbcTemplate jdbc;
    private FinanceiroInvestimentoRepositoryImpl repository;
    private Long idPessoa;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(System.getenv("NORD_TEST_DATABASE_URL"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_USER", "postgres"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_PASSWORD", ""));
        jdbc = new NamedParameterJdbcTemplate(ds);
        repository = new FinanceiroInvestimentoRepositoryImpl(jdbc);
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/query/financeiro_investimento.properties")) {
            props.load(in);
        }
        for (Field campo : FinanceiroInvestimentoRepositoryImpl.class.getDeclaredFields()) {
            Value valor = campo.getAnnotation(Value.class);
            if (valor != null) ReflectionTestUtils.setField(repository, campo.getName(), props.getProperty(valor.value().replace("${", "").replace("}", "")));
        }
        idPessoa = jdbc.queryForObject("SELECT id_pessoa FROM financeiro_pessoa WHERE nm_pessoa = 'Nick'", new MapSqlParameterSource(), Long.class);
        limpar();
    }

    @AfterEach
    void limpar() {
        MapSqlParameterSource p = new MapSqlParameterSource();
        jdbc.update("DELETE FROM financeiro_provento WHERE id_ativo IN (SELECT id_ativo FROM financeiro_ativo WHERE cd_ticker LIKE 'TEST%')", p);
        jdbc.update("DELETE FROM financeiro_operacao WHERE id_ativo IN (SELECT id_ativo FROM financeiro_ativo WHERE cd_ticker LIKE 'TEST%')", p);
        jdbc.update("DELETE FROM financeiro_ativo WHERE cd_ticker LIKE 'TEST%'", p);
    }

    private Long novoAtivo(String ticker) {
        FinanceiroAtivo a = new FinanceiroAtivo();
        a.setCdTicker(ticker);
        a.setNmAtivo("Fundo de teste");
        a.setIdPessoa(idPessoa);
        return repository.inserirAtivo(a);
    }

    private FinanceiroOperacao operacao(Long idAtivo, String tipo, int cotas) {
        FinanceiroOperacao o = new FinanceiroOperacao();
        o.setCdRequisicao(UUID.randomUUID().toString());
        o.setIdAtivo(idAtivo);
        o.setDtOperacao(LocalDate.of(2026, 9, 1));
        o.setCdTipo(tipo);
        o.setQtCotas(cotas);
        o.setVlPreco(new BigDecimal("150.00"));
        return o;
    }

    @Test
    void ativoInsereBuscaListaEAlteraComVersao() {
        Long id = novoAtivo("TEST11");
        FinanceiroAtivo a = repository.buscarAtivo(id).get();
        assertEquals("TEST11", a.getCdTicker());
        assertEquals("Nick", a.getNmPessoa());
        assertEquals(1, a.getNrVersao());
        assertTrue(repository.buscarAtivoPorTicker("TEST11", idPessoa).isPresent());
        assertTrue(repository.listarAtivos(idPessoa).stream().anyMatch(x -> x.getId().equals(id)));
        assertTrue(repository.listarAtivos(null).stream().anyMatch(x -> x.getId().equals(id)));

        a.setNmAtivo("Renomeado");
        assertEquals(1, repository.alterarAtivo(a, 1));
        assertEquals(0, repository.alterarAtivo(a, 1));
        assertEquals(2, repository.buscarAtivo(id).get().getNrVersao());

        a.setInAtivo(false);
        repository.alterarAtivo(a, 2);
        assertFalse(repository.listarAtivos(null).stream().anyMatch(x -> x.getId().equals(id)));
    }

    @Test
    void cotacaoGuardada() {
        Long id = novoAtivo("TEST11");
        repository.atualizarCotacao(id, new BigDecimal("158.42"), LocalDateTime.of(2026, 10, 8, 11, 30));
        FinanceiroAtivo a = repository.buscarAtivo(id).get();
        assertEquals(new BigDecimal("158.42"), a.getVlCotacao());
        assertEquals(LocalDateTime.of(2026, 10, 8, 11, 30), a.getDhCotacao());
    }

    @Test
    void operacaoIdempotentePorRequisicao() {
        Long id = novoAtivo("TEST11");
        FinanceiroOperacao o = operacao(id, "COMPRA", 10);
        assertTrue(repository.inserirOperacao(o).isPresent());
        assertFalse(repository.inserirOperacao(o).isPresent());
        assertEquals(id, repository.buscarAtivoDaRequisicao(o.getCdRequisicao()).get());
        List<FinanceiroOperacao> lista = repository.listarOperacoes(Collections.singletonList(id));
        assertEquals(1, lista.size());
        assertEquals(10, lista.get(0).getQtCotas());
        assertEquals(LocalDate.of(2026, 9, 1), lista.get(0).getDtOperacao());
        repository.excluirOperacao(lista.get(0).getId());
        assertTrue(repository.listarOperacoes(Collections.singletonList(id)).isEmpty());
    }

    @Test
    void bancoRecusaVendaZeradaEPrecoInvalido() {
        Long id = novoAtivo("TEST11");
        boolean recusou = false;
        try {
            repository.inserirOperacao(operacao(id, "COMPRA", 0));
        } catch (RuntimeException ex) {
            recusou = true;
        }
        assertTrue(recusou);
    }

    @Test
    void proventoManualSobrescreveEImportadoNao() {
        Long id = novoAtivo("TEST11");
        FinanceiroProvento p = new FinanceiroProvento();
        p.setIdAtivo(id);
        p.setDtCom(LocalDate.of(2026, 9, 30));
        p.setDtPagamento(LocalDate.of(2026, 10, 15));
        p.setVlPorCota(new BigDecimal("1.10"));

        assertTrue(repository.gravarProventoImportado(p));
        p.setVlPorCota(new BigDecimal("1.20"));
        assertTrue(repository.gravarProventoImportado(p));
        assertEquals(0, new BigDecimal("1.20").compareTo(repository.listarProventos(Collections.singletonList(id)).get(0).getVlPorCota()));

        p.setVlPorCota(new BigDecimal("1.30"));
        repository.gravarProventoManual(p);
        FinanceiroProvento gravado = repository.listarProventos(Collections.singletonList(id)).get(0);
        assertEquals("MANUAL", gravado.getCdOrigem());

        p.setVlPorCota(new BigDecimal("9.99"));
        assertFalse(repository.gravarProventoImportado(p));
        assertEquals(0, new BigDecimal("1.30").compareTo(repository.listarProventos(Collections.singletonList(id)).get(0).getVlPorCota()));

        Optional<FinanceiroProvento> porId = repository.buscarProvento(gravado.getId());
        assertTrue(porId.isPresent());
        repository.excluirProvento(gravado.getId());
        assertTrue(repository.listarProventos(Collections.singletonList(id)).isEmpty());
    }
}
