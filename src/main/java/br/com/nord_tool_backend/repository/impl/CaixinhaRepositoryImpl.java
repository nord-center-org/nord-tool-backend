package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import br.com.nord_tool_backend.domain.CaixinhaResponsavel;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.CaixinhaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository @Slf4j @RequiredArgsConstructor
@PropertySource("classpath:query/caixinha.properties")
public class CaixinhaRepositoryImpl implements CaixinhaRepository {

    private static final String ORDEM_LANCAMENTOS = " ORDER BY l.dt_lancamento DESC, l.id_lancamento DESC";

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${SPS.CAIXINHA.RESPONSAVEL.LISTAR}") private String qRespListar;
    @Value("${SPS.CAIXINHA.RESPONSAVEL.BUSCAR}") private String qRespBuscar;
    @Value("${SPI.CAIXINHA.RESPONSAVEL.INSERIR}") private String qRespInserir;
    @Value("${SPU.CAIXINHA.RESPONSAVEL.ALTERAR}") private String qRespAlterar;

    @Value("${SPS.CAIXINHA.LANCAMENTO.LISTAR}") private String qLancListar;
    @Value("${SPS.CAIXINHA.LANCAMENTO.BUSCAR}") private String qLancBuscar;
    @Value("${SPS.CAIXINHA.LANCAMENTO.BUSCAR_REQUISICAO}") private String qLancRequisicao;
    @Value("${SPI.CAIXINHA.LANCAMENTO.INSERIR}") private String qLancInserir;
    @Value("${SPU.CAIXINHA.LANCAMENTO.ALTERAR}") private String qLancAlterar;
    @Value("${SPU.CAIXINHA.LANCAMENTO.MARCAR}") private String qLancMarcar;
    @Value("${SPD.CAIXINHA.LANCAMENTO.DELETAR}") private String qLancDeletar;
    @Value("${SPS.CAIXINHA.LANCAMENTO.RESUMO}") private String qLancResumo;

    @Value("${SPS.CAIXINHA.COMPROVANTE.LISTAR}") private String qCompListar;
    @Value("${SPS.CAIXINHA.COMPROVANTE.BUSCAR}") private String qCompBuscar;
    @Value("${SPS.CAIXINHA.COMPROVANTE.BUSCAR_REQUISICAO}") private String qCompRequisicao;
    @Value("${SPI.CAIXINHA.COMPROVANTE.INSERIR}") private String qCompInserir;
    @Value("${SPD.CAIXINHA.COMPROVANTE.DELETAR}") private String qCompDeletar;

    // ---------- responsáveis ----------

    @Override
    public List<CaixinhaResponsavel> listarResponsaveis() {
        return executar("Erro ao listar responsáveis", () ->
                jdbc.query(qRespListar, BeanPropertyRowMapper.newInstance(CaixinhaResponsavel.class)));
    }

    @Override
    public Optional<CaixinhaResponsavel> buscarResponsavel(Long id) {
        return executar("Erro ao buscar responsável", () ->
                jdbc.query(qRespBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaResponsavel.class)).stream().findFirst());
    }

    @Override
    public Long inserirResponsavel(CaixinhaResponsavel r) {
        return executar("Erro ao salvar responsável", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qRespInserir, new BeanPropertySqlParameterSource(r), keys, new String[]{"id_responsavel"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void alterarResponsavel(CaixinhaResponsavel r) {
        executar("Erro ao alterar responsável", () -> jdbc.update(qRespAlterar, new BeanPropertySqlParameterSource(r)));
    }

    // ---------- lançamentos ----------

    @Override
    public List<CaixinhaLancamento> listarLancamentos(CaixinhaFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancListar + condicoes(filtro, params) + ORDEM_LANCAMENTOS;
        return executar("Erro ao listar lançamentos", () ->
                jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(CaixinhaLancamento.class)));
    }

    @Override
    public Optional<CaixinhaLancamento> buscarLancamento(Long id) {
        return executar("Erro ao buscar lançamento", () ->
                jdbc.query(qLancBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaLancamento.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarLancamentoPorRequisicao(String cdRequisicao) {
        return executar("Erro ao buscar lançamento", () ->
                jdbc.queryForList(qLancRequisicao, new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class)
                        .stream().findFirst());
    }

    @Override
    public Optional<Long> inserirLancamento(CaixinhaLancamento l) {
        return executar("Erro ao salvar lançamento", () ->
                jdbc.queryForList(qLancInserir, new MapSqlParameterSource()
                        .addValue("cdRequisicao", l.getCdRequisicao())
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idResponsavel", l.getIdResponsavel())
                        .addValue("txInsumo", l.getTxInsumo())
                        .addValue("vlValor", l.getVlValor())
                        .addValue("inLancado", Boolean.TRUE.equals(l.getInLancado()))
                        .addValue("inPago", Boolean.TRUE.equals(l.getInPago())), Long.class)
                        .stream().findFirst());
    }

    @Override
    public int alterarLancamento(CaixinhaLancamento l, int nrVersao) {
        return executar("Erro ao alterar lançamento", () ->
                jdbc.update(qLancAlterar, new MapSqlParameterSource()
                        .addValue("id", l.getId())
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idResponsavel", l.getIdResponsavel())
                        .addValue("txInsumo", l.getTxInsumo())
                        .addValue("vlValor", l.getVlValor())
                        .addValue("inLancado", Boolean.TRUE.equals(l.getInLancado()))
                        .addValue("inPago", Boolean.TRUE.equals(l.getInPago()))
                        .addValue("nrVersao", nrVersao)));
    }

    @Override
    public int marcarLancamento(Long id, boolean lancado, boolean pago, int nrVersao) {
        return executar("Erro ao atualizar lançamento", () ->
                jdbc.update(qLancMarcar, new MapSqlParameterSource().addValue("id", id)
                        .addValue("inLancado", lancado).addValue("inPago", pago).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int deletarLancamento(Long id, int nrVersao) {
        return executar("Erro ao excluir lançamento", () ->
                jdbc.update(qLancDeletar, new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Totais resumir(CaixinhaFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancResumo + condicoes(filtro, params);
        return executar("Erro ao calcular o resumo da Caixinha", () ->
                jdbc.queryForObject(sql, params, (rs, i) -> new Totais(
                        rs.getBigDecimal("total"), rs.getBigDecimal("pago"), rs.getInt("qtLancamentos"), rs.getInt("qtPagos"))));
    }

    /** Condições AND dos filtros; valores sempre por parâmetro nomeado (sem concatenar texto do cliente). */
    private static String condicoes(CaixinhaFiltro filtro, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder();
        if (filtro == null) return "";
        if (filtro.getResponsavel() != null && !filtro.getResponsavel().trim().isEmpty()) {
            sb.append(" AND r.nm_responsavel = :responsavel");
            params.addValue("responsavel", filtro.getResponsavel().trim());
        }
        String situacao = filtro.getSituacao() == null ? "TODOS" : filtro.getSituacao();
        if ("A_PAGAR".equals(situacao)) sb.append(" AND l.in_pago = FALSE");
        else if ("PAGO".equals(situacao)) sb.append(" AND l.in_pago = TRUE");
        else if ("NAO_LANCADO".equals(situacao)) sb.append(" AND l.in_lancado = FALSE");
        if (filtro.getDe() != null) {
            sb.append(" AND l.dt_lancamento >= :de");
            params.addValue("de", Date.valueOf(filtro.getDe()));
        }
        if (filtro.getAte() != null) {
            sb.append(" AND l.dt_lancamento <= :ate");
            params.addValue("ate", Date.valueOf(filtro.getAte()));
        }
        return sb.toString();
    }

    // ---------- comprovantes ----------

    @Override
    public List<CaixinhaComprovante> listarComprovantes(Long idLancamento) {
        return executar("Erro ao listar comprovantes", () ->
                jdbc.query(qCompListar, new MapSqlParameterSource("idLancamento", idLancamento),
                        BeanPropertyRowMapper.newInstance(CaixinhaComprovante.class)));
    }

    @Override
    public Optional<CaixinhaComprovante> buscarComprovante(Long id) {
        return executar("Erro ao buscar comprovante", () ->
                jdbc.query(qCompBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaComprovante.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarComprovantePorRequisicao(String cdRequisicao) {
        return executar("Erro ao buscar comprovante", () ->
                jdbc.queryForList(qCompRequisicao, new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class)
                        .stream().findFirst());
    }

    @Override
    public Long inserirComprovante(Long idLancamento, Long idArquivo, String cdRequisicao) {
        return executar("Erro ao salvar comprovante", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            SqlParameterSource params = new MapSqlParameterSource().addValue("idLancamento", idLancamento)
                    .addValue("idArquivo", idArquivo).addValue("cdRequisicao", cdRequisicao);
            jdbc.update(qCompInserir, params, keys, new String[]{"id_comprovante"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void deletarComprovante(Long id) {
        executar("Erro ao excluir comprovante", () -> jdbc.update(qCompDeletar, new MapSqlParameterSource("id", id)));
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
