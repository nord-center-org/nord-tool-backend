package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executa as queries do Financeiro num PostgreSQL de TESTE (nunca produção) já com os scripts do
 * nord-tool-scripts-sql aplicados. Só roda com NORD_TEST_DATABASE_URL, ex.:
 * jdbc:postgresql://localhost:5432/nordtest?currentSchema=nord_tool (usuário/senha em NORD_TEST_DATABASE_USER/PASSWORD).
 */
@EnabledIfEnvironmentVariable(named = "NORD_TEST_DATABASE_URL", matches = ".+")
class FinanceiroRepositoryImplDbTest {

    private NamedParameterJdbcTemplate jdbc;
    private FinanceiroRepositoryImpl repository;
    private Long idPessoa;
    private Long idCategoriaSaida;
    private Long idCategoriaEntrada;
    private final List<Long> criados = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(System.getenv("NORD_TEST_DATABASE_URL"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_USER", "postgres"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_PASSWORD", ""));
        jdbc = new NamedParameterJdbcTemplate(ds);
        repository = new FinanceiroRepositoryImpl(jdbc);
        injetarQueries(repository);

        idPessoa = repository.listarPessoas().stream().filter(p -> "Nick".equals(p.getNmPessoa())).findFirst().get().getId();
        idCategoriaSaida = repository.listarCategorias().stream().filter(c -> "Fatura".equals(c.getNmCategoria())).findFirst().get().getId();
        idCategoriaEntrada = repository.listarCategorias().stream().filter(c -> "Salário".equals(c.getNmCategoria())).findFirst().get().getId();
    }

    @AfterEach
    void limpar() {
        for (Long id : criados) {
            jdbc.update("DELETE FROM financeiro_lancamento WHERE id_lancamento = :id", new MapSqlParameterSource("id", id));
        }
        jdbc.update("DELETE FROM financeiro_pessoa WHERE nm_pessoa LIKE 'Teste DB %'", new MapSqlParameterSource());
    }

    /** Preenche os campos @Value com o conteúdo de query/financeiro.properties (o Spring faz isso na aplicação). */
    private static void injetarQueries(Object alvo) throws Exception {
        Properties props = new Properties();
        try (InputStream in = FinanceiroRepositoryImplDbTest.class.getResourceAsStream("/query/financeiro.properties")) {
            props.load(in);
        }
        for (Field campo : alvo.getClass().getDeclaredFields()) {
            Value valor = campo.getAnnotation(Value.class);
            if (valor == null) continue;
            String chave = valor.value().replace("${", "").replace("}", "");
            ReflectionTestUtils.setField(alvo, campo.getName(), props.getProperty(chave));
        }
    }

    private FinanceiroLancamento novo(String descricao, Long idCategoria, String valor, LocalDate data, boolean realizado) {
        FinanceiroLancamento l = new FinanceiroLancamento();
        l.setCdRequisicao(UUID.randomUUID().toString());
        l.setDtCompetencia(data.withDayOfMonth(1));
        l.setDtLancamento(data);
        l.setIdCategoria(idCategoria);
        l.setIdPessoa(idPessoa);
        l.setDsLancamento(descricao);
        l.setVlLancamento(new BigDecimal(valor));
        l.setInRealizado(realizado);
        return l;
    }

    private Long inserir(FinanceiroLancamento l) {
        Long id = repository.inserirLancamento(l).get();
        criados.add(id);
        return id;
    }

    @Test
    void cadastrosIniciaisDoScriptEstaoLa() {
        assertTrue(repository.listarPessoas().stream().anyMatch(p -> p.getInCompartilhado()));
        assertTrue(repository.listarCategorias().stream().anyMatch(c -> "RITMO_FATURA".equals(c.getCdProjecao())));
        assertEquals("DIA_UTIL", repository.buscarCategoria(idCategoriaEntrada).get().getCdRegraData());
    }

    @Test
    void insereBuscaEFiltraLancamentos() {
        LocalDate dia = LocalDate.of(2031, 3, 8);
        Long fatura = inserir(novo("Fatura do cartão DBTEST", idCategoriaSaida, "1500.50", dia, false));
        Long salario = inserir(novo("Salário DBTEST", idCategoriaEntrada, "5000.00", dia.plusDays(2), true));

        FinanceiroLancamento lido = repository.buscarLancamento(fatura).get();
        assertEquals("Fatura", lido.getNmCategoria());
        assertEquals("SAIDA", lido.getCdTipo());
        assertEquals("Nick", lido.getNmPessoa());
        assertEquals(0, new BigDecimal("1500.50").compareTo(lido.getVlLancamento()));
        assertEquals(1, lido.getNrVersao());
        assertEquals(LocalDate.of(2031, 3, 1), lido.getDtCompetencia());

        FinanceiroFiltro mes = new FinanceiroFiltro("2031-03", null, null, null, null, null, null, null);
        assertEquals(2, repository.listarLancamentos(mes).size());
        // ordenação: data mais recente primeiro
        assertEquals(salario, repository.listarLancamentos(mes).get(0).getId());

        assertEquals(1, repository.listarLancamentos(new FinanceiroFiltro("2031-03", null, null, null, null, "SAIDA", null, null)).size());
        assertEquals(1, repository.listarLancamentos(new FinanceiroFiltro("2031-03", null, null, null, null, null, "REALIZADO", null)).size());
        assertEquals(fatura, repository.listarLancamentos(new FinanceiroFiltro("2031-03", null, null, null, null, null, "PREVISTO", null)).get(0).getId());
        assertEquals(1, repository.listarLancamentos(new FinanceiroFiltro("2031-03", null, null, null, idCategoriaEntrada, null, null, null)).size());
        assertEquals(2, repository.listarLancamentos(new FinanceiroFiltro("2031-03", null, null, idPessoa, null, null, null, null)).size());
        assertEquals(1, repository.listarLancamentos(new FinanceiroFiltro(null, dia.plusDays(1), dia.plusDays(5), null, null, null, null, "dbtest")).size());
        assertEquals(0, repository.listarLancamentos(new FinanceiroFiltro("2031-04", null, null, null, null, null, null, null)).size());
    }

    @Test
    void buscaPorTextoAchaDescricaoCategoriaEPessoaEIgnoraCuringas() {
        LocalDate dia = LocalDate.of(2031, 5, 8);
        inserir(novo("desconto de 100% DBTEST", idCategoriaSaida, "10.00", dia, false));
        inserir(novo("sem curinga DBTEST", idCategoriaSaida, "20.00", dia, false));

        FinanceiroFiltro porPorcento = new FinanceiroFiltro("2031-05", null, null, null, null, null, null, "100%");
        assertEquals(1, repository.listarLancamentos(porPorcento).size());
        assertEquals(2, repository.listarLancamentos(new FinanceiroFiltro("2031-05", null, null, null, null, null, null, "fatura")).size());
        assertEquals(2, repository.listarLancamentos(new FinanceiroFiltro("2031-05", null, null, null, null, null, null, "NICK")).size());
        // "_" não é coringa de um caractere
        assertEquals(0, repository.listarLancamentos(new FinanceiroFiltro("2031-05", null, null, null, null, null, null, "sem_curinga")).size());
    }

    @Test
    void criacaoIdempotentePorRequisicao() {
        FinanceiroLancamento l = novo("Idempotente DBTEST", idCategoriaSaida, "1.00", LocalDate.of(2031, 6, 8), false);
        Long id = inserir(l);

        assertFalse(repository.inserirLancamento(l).isPresent());
        assertEquals(Optional.of(id), repository.buscarLancamentoPorRequisicao(l.getCdRequisicao()));
    }

    @Test
    void controleDeVersaoNaEdicaoMarcacaoEExclusao() {
        Long id = inserir(novo("Versao DBTEST", idCategoriaSaida, "30.00", LocalDate.of(2031, 7, 8), false));
        FinanceiroLancamento edicao = repository.buscarLancamento(id).get();
        edicao.setDsLancamento("Versao editada DBTEST");
        edicao.setVlLancamento(new BigDecimal("31.00"));

        assertEquals(1, repository.alterarLancamento(edicao, 1));
        assertEquals(0, repository.alterarLancamento(edicao, 1), "versão antiga não pode alterar");
        FinanceiroLancamento depois = repository.buscarLancamento(id).get();
        assertEquals(2, depois.getNrVersao());
        assertEquals("Versao editada DBTEST", depois.getDsLancamento());

        assertEquals(0, repository.marcarRealizado(id, true, 1));
        assertEquals(1, repository.marcarRealizado(id, true, 2));
        assertTrue(repository.buscarLancamento(id).get().getInRealizado());

        assertEquals(0, repository.deletarLancamento(id, 2));
        assertEquals(1, repository.deletarLancamento(id, 3));
        assertFalse(repository.buscarLancamento(id).isPresent());
    }

    @Test
    void resumoSomaEntradasESaidas() {
        LocalDate dia = LocalDate.of(2031, 8, 8);
        inserir(novo("a DBTEST", idCategoriaEntrada, "5000.00", dia, true));
        inserir(novo("b DBTEST", idCategoriaSaida, "1500.50", dia, false));
        inserir(novo("c DBTEST", idCategoriaSaida, "499.50", dia, true));

        FinanceiroRepository.Totais t = repository.resumir(new FinanceiroFiltro("2031-08", null, null, null, null, null, null, null));

        assertEquals(0, new BigDecimal("5000.00").compareTo(t.entradas));
        assertEquals(0, new BigDecimal("2000.00").compareTo(t.saidas));
        assertEquals(0, new BigDecimal("5000.00").compareTo(t.entradasRealizadas));
        assertEquals(0, new BigDecimal("499.50").compareTo(t.saidasRealizadas));
        assertEquals(3, t.qtLancamentos);
        assertEquals(2, t.qtRealizados);

        FinanceiroRepository.Totais vazio = repository.resumir(new FinanceiroFiltro("2031-09", null, null, null, null, null, null, null));
        assertEquals(0, BigDecimal.ZERO.compareTo(vazio.entradas));
        assertEquals(0, vazio.qtLancamentos);
    }

    @Test
    void pessoasECategoriasComContagemDeUso() {
        FinanceiroPessoa p = new FinanceiroPessoa();
        p.setNmPessoa("Teste DB pessoa");
        p.setInCompartilhado(false);
        p.setNrOrdem(99);
        p.setInAtivo(true);
        Long id = repository.inserirPessoa(p);

        FinanceiroPessoa lida = repository.buscarPessoa(id).get();
        assertEquals("Teste DB pessoa", lida.getNmPessoa());
        lida.setInAtivo(false);
        repository.alterarPessoa(lida);
        assertFalse(repository.buscarPessoa(id).get().getInAtivo());

        int antes = repository.contarLancamentosDaCategoria(idCategoriaSaida);
        inserir(novo("contagem DBTEST", idCategoriaSaida, "1.00", LocalDate.of(2031, 10, 8), false));
        assertEquals(antes + 1, repository.contarLancamentosDaCategoria(idCategoriaSaida));
    }
}
