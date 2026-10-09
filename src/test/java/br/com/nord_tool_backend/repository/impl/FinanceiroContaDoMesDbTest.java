package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoLinhaDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroLancamentoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;
import br.com.nord_tool_backend.service.impl.FinanceiroProjecaoServiceImpl;
import br.com.nord_tool_backend.service.impl.FinanceiroServiceImpl;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A conta do mês de ponta a ponta contra o PostgreSQL de TESTE (nunca produção), com os serviços e repositórios reais:
 * lançar, fechar, travar, gerar os fixos, projetar a fatura e reabrir. Usa só meses de 2032. Só roda com
 * NORD_TEST_DATABASE_URL (veja {@link FinanceiroRepositoryImplDbTest}).
 */
@EnabledIfEnvironmentVariable(named = "NORD_TEST_DATABASE_URL", matches = ".+")
class FinanceiroContaDoMesDbTest {

    private NamedParameterJdbcTemplate jdbc;
    private FinanceiroServiceImpl lancamentos;
    private FinanceiroProjecaoServiceImpl conta;
    private Long idNick;
    private Long idUsuario;
    private Long idSalario;
    private Long idFatura;
    private Long idApartamento;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(System.getenv("NORD_TEST_DATABASE_URL"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_USER", "postgres"),
                System.getenv().getOrDefault("NORD_TEST_DATABASE_PASSWORD", ""));
        jdbc = new NamedParameterJdbcTemplate(ds);
        FinanceiroRepositoryImpl repo = new FinanceiroRepositoryImpl(jdbc);
        FinanceiroProjecaoRepositoryImpl projecao = new FinanceiroProjecaoRepositoryImpl(jdbc);
        injetar(repo, "/query/financeiro.properties");
        injetar(projecao, "/query/financeiro_projecao.properties");
        // 08/03/2032 em São Paulo: o mês atual é março de 2032
        Clock relogio = Clock.fixed(Instant.parse("2032-03-28T15:00:00Z"), ZoneId.of("UTC"));
        lancamentos = new FinanceiroServiceImpl(repo, projecao, relogio);
        conta = new FinanceiroProjecaoServiceImpl(repo, projecao, relogio);

        limpar();
        // usuário de teste: o autor do lançamento e de quem fechou o mês têm chave estrangeira para a tabela usuario
        idUsuario = jdbc.queryForObject("INSERT INTO usuario (nm_email, nm_nome, nm_senha_hash, id_perfil) "
                + "SELECT 'financeiro-db-test@nord.test', 'Teste DB', 'x', id_perfil FROM perfil WHERE cd_perfil = 'ADMIN' RETURNING id_usuario",
                new MapSqlParameterSource(), Long.class);
        idNick = repo.listarPessoas().stream().filter(p -> "Nick".equals(p.getNmPessoa())).map(FinanceiroPessoa::getId).findFirst().get();
        idSalario = categoria(repo.listarCategorias(), "Salário");
        idFatura = categoria(repo.listarCategorias(), "Fatura");
        idApartamento = categoria(repo.listarCategorias(), "Apartamento");
    }

    @AfterEach
    void limpar() {
        MapSqlParameterSource p = new MapSqlParameterSource();
        jdbc.update("DELETE FROM financeiro_fatura_leitura WHERE id_lancamento IN (SELECT id_lancamento FROM financeiro_lancamento "
                + "WHERE dt_competencia >= DATE '2032-01-01' AND dt_competencia < DATE '2033-01-01')", p);
        jdbc.update("DELETE FROM financeiro_lancamento WHERE dt_competencia >= DATE '2032-01-01' AND dt_competencia < DATE '2033-01-01'", p);
        jdbc.update("DELETE FROM financeiro_mes WHERE dt_competencia >= DATE '2032-01-01' AND dt_competencia < DATE '2033-01-01'", p);
        jdbc.update("DELETE FROM financeiro_recorrencia WHERE dt_inicio >= DATE '2032-01-01'", p);
        jdbc.update("UPDATE financeiro_configuracao SET vl_meta_saldo = 0, nr_meses_media = 3, nr_dia_conferencia = 8, nr_dia_fechamento_fatura = 8", p);
        jdbc.update("DELETE FROM usuario WHERE nm_email = 'financeiro-db-test@nord.test'", p);
    }

    private static void injetar(Object alvo, String arquivo) throws Exception {
        Properties props = new Properties();
        try (InputStream in = FinanceiroContaDoMesDbTest.class.getResourceAsStream(arquivo)) {
            props.load(in);
        }
        for (Field campo : alvo.getClass().getDeclaredFields()) {
            Value valor = campo.getAnnotation(Value.class);
            if (valor == null) continue;
            ReflectionTestUtils.setField(alvo, campo.getName(), props.getProperty(valor.value().replace("${", "").replace("}", "")));
        }
    }

    private static Long categoria(List<FinanceiroCategoria> categorias, String nome) {
        return categorias.stream().filter(c -> nome.equals(c.getNmCategoria())).map(FinanceiroCategoria::getId).findFirst().get();
    }

    private FinanceiroLancamentoDto lancar(Long idCategoria, String data, String valor, boolean realizado) {
        FinanceiroLancamentoForm f = new FinanceiroLancamentoForm();
        f.setCdRequisicao(UUID.randomUUID().toString());
        f.setDtLancamento(LocalDate.parse(data));
        f.setIdCategoria(idCategoria);
        f.setIdPessoa(idNick);
        f.setVlLancamento(new BigDecimal(valor));
        f.setInRealizado(realizado);
        return lancamentos.criar(f, idUsuario).get(0);
    }

    private static FinanceiroProjecaoLinhaDto linha(List<FinanceiroProjecaoLinhaDto> linhas, Long idCategoria) {
        return linhas.stream().filter(l -> l.getIdCategoria().equals(idCategoria)).findFirst().get();
    }

    private void configurar(String meta, int meses, int diaFatura) {
        FinanceiroConfiguracaoForm cfg = new FinanceiroConfiguracaoForm();
        cfg.setVlMetaSaldo(new BigDecimal(meta));
        cfg.setNrMesesMedia(meses);
        cfg.setNrDiaConferencia(8);
        cfg.setNrDiaFechamentoFatura(diaFatura);
        conta.atualizarConfiguracao(cfg);
    }

    @Test
    void contaDoMesDeFevereiroAMarcoDeCimaAbaixo() {
        // fatura fecha dia 20: o ciclo da fatura de março (a das compras de março) vai de 21/03 a 20/04 (31 dias)
        configurar("500.00", 3, 20);
        FinanceiroSaldoInicialForm saldoInicial = new FinanceiroSaldoInicialForm();
        saldoInicial.setVlSaldoInicial(new BigDecimal("100.00"));
        conta.definirSaldoInicial("2032-02", saldoInicial);

        // fevereiro: entrou o salário, saíram apartamento e fatura
        lancar(idSalario, "2032-02-05", "3000.00", true);
        lancar(idApartamento, "2032-02-10", "1000.00", true);
        lancar(idFatura, "2032-02-08", "2000.00", true);
        FinanceiroProjecaoMesDto fev = conta.obterMes("2032-02", null);
        assertEquals(new BigDecimal("100.00"), fev.getSaldoAnterior());
        assertEquals(0, new BigDecimal("100.00").compareTo(fev.getSaldoFinal()));
        assertFalse(fev.isEstimado(), "fevereiro já passou: só vale o lançado");

        // recorrência do apartamento: será gerada em março quando fevereiro fechar
        FinanceiroRecorrenciaForm rec = new FinanceiroRecorrenciaForm();
        rec.setIdCategoria(idApartamento);
        rec.setIdPessoa(idNick);
        rec.setDsRecorrencia("Parcela do apartamento");
        rec.setVlRecorrencia(new BigDecimal("1000.00"));
        rec.setNrDia(10);
        rec.setDtInicio(LocalDate.of(2032, 1, 1));
        conta.criarRecorrencia(rec);

        FinanceiroFechamentoDto fechamento = conta.fechar("2032-02", idUsuario);
        assertTrue(fechamento.getMes().isFechado());
        assertEquals(0, new BigDecimal("100.00").compareTo(fechamento.getMes().getSaldoFinal()));
        assertEquals(1, fechamento.getRecorrenciasGeradas(), "o apartamento de março foi gerado");

        // reenvio da geração não duplica
        assertEquals(0, conta.gerarRecorrencias("2032-03", idUsuario).getCriados());

        // fevereiro fechado trava lançamentos
        ValidacaoException travado = assertThrows(ValidacaoException.class, () -> lancar(idSalario, "2032-02-20", "10.00", false));
        assertTrue(travado.getMessage().contains("fechado"));

        // março (mês atual): salário e fatura parcial lançados; o resto é estimativa
        lancar(idSalario, "2032-03-05", "3000.00", true);
        lancar(idFatura, "2032-03-28", "600.00", false);

        FinanceiroProjecaoMesDto mar = conta.obterMes("2032-03", null);
        assertTrue(mar.isEstimado());
        assertEquals(0, new BigDecimal("100.00").compareTo(mar.getSaldoAnterior()), "vem do saldo gravado no fechamento de fevereiro");
        assertEquals(0, new BigDecimal("3000.00").compareTo(mar.getTotalEntradas()));
        // fatura: em 28/03 passaram 8 dos 31 dias: 600 + (1 - 8/31) * 2000 (média de fevereiro) = 2083,87; apartamento: o gerado (1000)
        assertEquals(0, new BigDecimal("2083.87").compareTo(linha(mar.getSaidas(), idFatura).getProjetado()));
        assertEquals("RITMO", linha(mar.getSaidas(), idFatura).getOrigem());
        assertEquals(0, new BigDecimal("1000.00").compareTo(linha(mar.getSaidas(), idApartamento).getProjetado()));
        assertEquals(0, new BigDecimal("3083.87").compareTo(mar.getTotalSaidas()));
        assertEquals(0, new BigDecimal("16.13").compareTo(mar.getSaldoFinal()));
        assertEquals(0, new BigDecimal("-483.87").compareTo(mar.getFolga()));
        assertEquals("VERMELHO", mar.getSituacao()); // 16,13 fica abaixo da meta de 500
        assertEquals(2, mar.getQtPrevistos(), "a fatura e o apartamento gerado ainda não foram pagos");

        // com filtro de pessoa não há saldo anterior
        assertFalse(conta.obterMes("2032-03", idNick).isComSaldoAnterior());

        // a fatura guardou a leitura do dia
        Long idLancamentoFatura = jdbc.queryForObject("SELECT id_lancamento FROM financeiro_lancamento WHERE dt_competencia = DATE '2032-03-01' AND id_categoria = :c",
                new MapSqlParameterSource("c", idFatura), Long.class);
        assertEquals(1, conta.listarLeituras(idLancamentoFatura).size());
        assertEquals(LocalDate.of(2032, 3, 28), conta.listarLeituras(idLancamentoFatura).get(0).getDtLeitura());

        // fechar março grava o saldo REAL (sem projetar a fatura): 100 + 3000 - (600 + 1000) = 1500
        FinanceiroFechamentoDto fechouMarco = conta.fechar("2032-03", idUsuario);
        assertEquals(0, new BigDecimal("1500.00").compareTo(fechouMarco.getMes().getSaldoFinal()));

        // só o último mês fechado pode ser reaberto
        assertThrows(ValidacaoException.class, () -> conta.reabrir("2032-02"));
        assertFalse(conta.reabrir("2032-03").isFechado());
        assertFalse(conta.reabrir("2032-02").isFechado());
        lancar(idSalario, "2032-02-20", "10.00", false); // destravou
    }

    @Test
    void fecharExigeOMesAnteriorFechado() {
        lancar(idSalario, "2032-01-05", "100.00", true);
        lancar(idSalario, "2032-02-05", "100.00", true);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> conta.fechar("2032-02", idUsuario));
        assertTrue(ex.getMessage().contains("janeiro de 2032"));
        conta.fechar("2032-01", idUsuario);
        conta.fechar("2032-02", idUsuario);
        assertThrows(ValidacaoException.class, () -> conta.fechar("2032-02", idUsuario), "já fechado");
    }

    @Test
    void configuracaoPadraoEAtualizacao() {
        assertEquals(8, conta.obterConfiguracao().getNrDiaFechamentoFatura());
        configurar("750.00", 6, 15);
        assertEquals(0, new BigDecimal("750.00").compareTo(conta.obterConfiguracao().getVlMetaSaldo()));
        assertEquals(6, conta.obterConfiguracao().getNrMesesMedia());
        assertEquals(15, conta.obterConfiguracao().getNrDiaFechamentoFatura());
    }
}
