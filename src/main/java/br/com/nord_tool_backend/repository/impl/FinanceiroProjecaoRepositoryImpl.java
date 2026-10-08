package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroFaturaLeitura;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository @Slf4j @RequiredArgsConstructor
@PropertySource("classpath:query/financeiro_projecao.properties")
public class FinanceiroProjecaoRepositoryImpl implements FinanceiroProjecaoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${SPS.FINANCEIRO.CONFIGURACAO.BUSCAR}") private String qConfBuscar;
    @Value("${SPI.FINANCEIRO.CONFIGURACAO.GARANTIR}") private String qConfGarantir;
    @Value("${SPU.FINANCEIRO.CONFIGURACAO.ALTERAR}") private String qConfAlterar;

    @Value("${SPS.FINANCEIRO.MES.BUSCAR}") private String qMesBuscar;
    @Value("${SPS.FINANCEIRO.MES.LISTAR}") private String qMesListar;
    @Value("${SPI.FINANCEIRO.MES.GARANTIR}") private String qMesGarantir;
    @Value("${SPU.FINANCEIRO.MES.SALDO_INICIAL}") private String qMesSaldoInicial;
    @Value("${SPU.FINANCEIRO.MES.FECHAR}") private String qMesFechar;
    @Value("${SPU.FINANCEIRO.MES.REABRIR}") private String qMesReabrir;
    @Value("${SPS.FINANCEIRO.MES.EXISTE_FECHADO_APOS}") private String qMesFechadoApos;
    @Value("${SPS.FINANCEIRO.MES.PRIMEIRA_COMPETENCIA}") private String qMesPrimeira;
    @Value("${SPS.FINANCEIRO.MES.CONTAR_PREVISTOS}") private String qMesPrevistos;

    @Value("${SPS.FINANCEIRO.SOMA.POR_CATEGORIA}") private String qSoma;

    @Value("${SPS.FINANCEIRO.FATURA.DO_MES}") private String qFaturaDoMes;
    @Value("${SPI.FINANCEIRO.FATURA.LEITURA}") private String qFaturaLeitura;
    @Value("${SPS.FINANCEIRO.FATURA.LEITURAS}") private String qFaturaLeituras;

    @Value("${SPS.FINANCEIRO.RECORRENCIA.LISTAR}") private String qRecListar;
    @Value("${SPI.FINANCEIRO.RECORRENCIA.INSERIR}") private String qRecInserir;
    @Value("${SPU.FINANCEIRO.RECORRENCIA.ALTERAR}") private String qRecAlterar;

    // ---------- configuração ----------

    @Override
    public Optional<FinanceiroConfiguracao> buscarConfiguracao() {
        return executar("Erro ao buscar a configuração", () ->
                jdbc.query(qConfBuscar, BeanPropertyRowMapper.newInstance(FinanceiroConfiguracao.class)).stream().findFirst());
    }

    @Override
    public void garantirConfiguracao() {
        executar("Erro ao criar a configuração", () -> jdbc.update(qConfGarantir, new MapSqlParameterSource()));
    }

    @Override
    public void alterarConfiguracao(FinanceiroConfiguracao c) {
        executar("Erro ao alterar a configuração", () -> jdbc.update(qConfAlterar, new MapSqlParameterSource()
                .addValue("vlMetaSaldo", c.getVlMetaSaldo())
                .addValue("nrMesesMedia", c.getNrMesesMedia())
                .addValue("nrDiaConferencia", c.getNrDiaConferencia())
                .addValue("nrDiaFechamentoFatura", c.getNrDiaFechamentoFatura())));
    }

    // ---------- meses ----------

    @Override
    public Optional<FinanceiroMes> buscarMes(LocalDate competencia) {
        return executar("Erro ao buscar o mês", () ->
                jdbc.query(qMesBuscar, new MapSqlParameterSource("competencia", Date.valueOf(competencia)),
                        BeanPropertyRowMapper.newInstance(FinanceiroMes.class)).stream().findFirst());
    }

    @Override
    public List<FinanceiroMes> listarMeses(LocalDate de, LocalDate ate) {
        return executar("Erro ao listar os meses", () ->
                jdbc.query(qMesListar, new MapSqlParameterSource().addValue("de", Date.valueOf(de)).addValue("ate", Date.valueOf(ate)),
                        BeanPropertyRowMapper.newInstance(FinanceiroMes.class)));
    }

    @Override
    public void garantirMes(LocalDate competencia) {
        executar("Erro ao criar o mês", () -> jdbc.update(qMesGarantir, new MapSqlParameterSource("competencia", Date.valueOf(competencia))));
    }

    @Override
    public int definirSaldoInicial(LocalDate competencia, BigDecimal vlSaldoInicial) {
        return executar("Erro ao salvar o saldo inicial", () -> jdbc.update(qMesSaldoInicial, new MapSqlParameterSource()
                .addValue("competencia", Date.valueOf(competencia)).addValue("vlSaldoInicial", vlSaldoInicial)));
    }

    @Override
    public int fecharMes(LocalDate competencia, BigDecimal vlSaldoFinal, Long idUsuario) {
        return executar("Erro ao fechar o mês", () -> jdbc.update(qMesFechar, new MapSqlParameterSource()
                .addValue("competencia", Date.valueOf(competencia)).addValue("vlSaldoFinal", vlSaldoFinal)
                .addValue("idUsuario", idUsuario)));
    }

    @Override
    public int reabrirMes(LocalDate competencia) {
        return executar("Erro ao reabrir o mês", () ->
                jdbc.update(qMesReabrir, new MapSqlParameterSource("competencia", Date.valueOf(competencia))));
    }

    @Override
    public boolean existeMesFechadoApos(LocalDate competencia) {
        return executar("Erro ao consultar os meses fechados", () -> {
            Integer total = jdbc.queryForObject(qMesFechadoApos, new MapSqlParameterSource("competencia", Date.valueOf(competencia)), Integer.class);
            return total != null && total > 0;
        });
    }

    @Override
    public Optional<LocalDate> primeiraCompetencia() {
        return executar("Erro ao consultar o primeiro mês", () -> {
            Date data = jdbc.queryForObject(qMesPrimeira, new MapSqlParameterSource(), Date.class);
            return Optional.ofNullable(data).map(Date::toLocalDate);
        });
    }

    @Override
    public int contarPrevistos(LocalDate competencia) {
        return executar("Erro ao contar lançamentos previstos", () -> {
            Integer total = jdbc.queryForObject(qMesPrevistos, new MapSqlParameterSource("competencia", Date.valueOf(competencia)), Integer.class);
            return total == null ? 0 : total;
        });
    }

    // ---------- somas ----------

    @Override
    public List<FinanceiroSomaMes> somarPorCategoria(LocalDate de, LocalDate ate, Long idPessoa) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("de", Date.valueOf(de)).addValue("ate", Date.valueOf(ate));
        String sql = qSoma + (idPessoa == null ? "" : " AND l.id_pessoa = :idPessoa") + " GROUP BY l.dt_competencia, l.id_categoria";
        if (idPessoa != null) params.addValue("idPessoa", idPessoa);
        return executar("Erro ao somar os lançamentos", () -> jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(FinanceiroSomaMes.class)));
    }

    // ---------- faturas ----------

    @Override
    public List<FinanceiroFaturaAberta> faturasDoMes(LocalDate competencia, Long idPessoa) {
        MapSqlParameterSource params = new MapSqlParameterSource("competencia", Date.valueOf(competencia));
        String sql = qFaturaDoMes + (idPessoa == null ? "" : " AND l.id_pessoa = :idPessoa");
        if (idPessoa != null) params.addValue("idPessoa", idPessoa);
        return executar("Erro ao buscar as faturas do mês", () -> jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(FinanceiroFaturaAberta.class)));
    }

    @Override
    public void registrarLeitura(Long idLancamento, LocalDate dtLeitura, BigDecimal vlLeitura, Long idUsuario) {
        executar("Erro ao registrar a leitura da fatura", () -> jdbc.update(qFaturaLeitura, new MapSqlParameterSource()
                .addValue("idLancamento", idLancamento).addValue("dtLeitura", Date.valueOf(dtLeitura))
                .addValue("vlLeitura", vlLeitura).addValue("idUsuario", idUsuario)));
    }

    @Override
    public List<FinanceiroFaturaLeitura> listarLeituras(Long idLancamento) {
        return executar("Erro ao listar as leituras da fatura", () ->
                jdbc.query(qFaturaLeituras, new MapSqlParameterSource("idLancamento", idLancamento),
                        BeanPropertyRowMapper.newInstance(FinanceiroFaturaLeitura.class)));
    }

    // ---------- recorrências ----------

    @Override
    public List<FinanceiroRecorrencia> listarRecorrencias() {
        return executar("Erro ao listar as recorrências", () -> jdbc.query(
                qRecListar + " ORDER BY c.cd_tipo, LOWER(c.nm_categoria), r.id_recorrencia",
                BeanPropertyRowMapper.newInstance(FinanceiroRecorrencia.class)));
    }

    @Override
    public Optional<FinanceiroRecorrencia> buscarRecorrencia(Long id) {
        return executar("Erro ao buscar a recorrência", () -> jdbc.query(qRecListar + " WHERE r.id_recorrencia = :id",
                new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(FinanceiroRecorrencia.class)).stream().findFirst());
    }

    @Override
    public Long inserirRecorrencia(FinanceiroRecorrencia r) {
        return executar("Erro ao salvar a recorrência", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qRecInserir, parametros(r), keys, new String[]{"id_recorrencia"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public int alterarRecorrencia(FinanceiroRecorrencia r, int nrVersao) {
        return executar("Erro ao alterar a recorrência", () ->
                jdbc.update(qRecAlterar, parametros(r).addValue("id", r.getId()).addValue("nrVersao", nrVersao)));
    }

    private static MapSqlParameterSource parametros(FinanceiroRecorrencia r) {
        return new MapSqlParameterSource()
                .addValue("idCategoria", r.getIdCategoria()).addValue("idPessoa", r.getIdPessoa())
                .addValue("dsRecorrencia", r.getDsRecorrencia()).addValue("vlRecorrencia", r.getVlRecorrencia())
                .addValue("nrDia", r.getNrDia()).addValue("dtInicio", Date.valueOf(r.getDtInicio()))
                .addValue("dtFim", r.getDtFim() == null ? null : Date.valueOf(r.getDtFim()))
                .addValue("inAtivo", !Boolean.FALSE.equals(r.getInAtivo()));
    }

    private <T> T executar(String mensagem, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (ValidacaoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(mensagem, ex);
            throw new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}
