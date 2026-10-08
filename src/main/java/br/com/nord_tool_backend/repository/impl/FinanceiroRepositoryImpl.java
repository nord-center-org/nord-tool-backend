package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository @Slf4j @RequiredArgsConstructor
@PropertySource("classpath:query/financeiro.properties")
public class FinanceiroRepositoryImpl implements FinanceiroRepository {

    private static final String ORDEM_LANCAMENTOS = " ORDER BY l.dt_lancamento DESC, l.id_lancamento DESC";

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${SPS.FINANCEIRO.PESSOA.LISTAR}") private String qPessoaListar;
    @Value("${SPS.FINANCEIRO.PESSOA.BUSCAR}") private String qPessoaBuscar;
    @Value("${SPI.FINANCEIRO.PESSOA.INSERIR}") private String qPessoaInserir;
    @Value("${SPU.FINANCEIRO.PESSOA.ALTERAR}") private String qPessoaAlterar;

    @Value("${SPS.FINANCEIRO.CATEGORIA.LISTAR}") private String qCategListar;
    @Value("${SPS.FINANCEIRO.CATEGORIA.BUSCAR}") private String qCategBuscar;
    @Value("${SPI.FINANCEIRO.CATEGORIA.INSERIR}") private String qCategInserir;
    @Value("${SPU.FINANCEIRO.CATEGORIA.ALTERAR}") private String qCategAlterar;
    @Value("${SPS.FINANCEIRO.CATEGORIA.CONTAR_LANCAMENTOS}") private String qCategContar;

    @Value("${SPS.FINANCEIRO.LANCAMENTO.LISTAR}") private String qLancListar;
    @Value("${SPS.FINANCEIRO.LANCAMENTO.BUSCAR}") private String qLancBuscar;
    @Value("${SPS.FINANCEIRO.LANCAMENTO.BUSCAR_REQUISICAO}") private String qLancRequisicao;
    @Value("${SPI.FINANCEIRO.LANCAMENTO.INSERIR}") private String qLancInserir;
    @Value("${SPU.FINANCEIRO.LANCAMENTO.ALTERAR}") private String qLancAlterar;
    @Value("${SPU.FINANCEIRO.LANCAMENTO.REALIZADO}") private String qLancRealizado;
    @Value("${SPD.FINANCEIRO.LANCAMENTO.DELETAR}") private String qLancDeletar;
    @Value("${SPS.FINANCEIRO.LANCAMENTO.RESUMO}") private String qLancResumo;

    // ---------- pessoas ----------

    @Override
    public List<FinanceiroPessoa> listarPessoas() {
        return executar("Erro ao listar pessoas", () ->
                jdbc.query(qPessoaListar, BeanPropertyRowMapper.newInstance(FinanceiroPessoa.class)));
    }

    @Override
    public Optional<FinanceiroPessoa> buscarPessoa(Long id) {
        return executar("Erro ao buscar pessoa", () ->
                jdbc.query(qPessoaBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(FinanceiroPessoa.class)).stream().findFirst());
    }

    @Override
    public Long inserirPessoa(FinanceiroPessoa p) {
        return executar("Erro ao salvar pessoa", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qPessoaInserir, new BeanPropertySqlParameterSource(p), keys, new String[]{"id_pessoa"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void alterarPessoa(FinanceiroPessoa p) {
        executar("Erro ao alterar pessoa", () -> jdbc.update(qPessoaAlterar, new BeanPropertySqlParameterSource(p)));
    }

    // ---------- categorias ----------

    @Override
    public List<FinanceiroCategoria> listarCategorias() {
        return executar("Erro ao listar categorias", () ->
                jdbc.query(qCategListar, BeanPropertyRowMapper.newInstance(FinanceiroCategoria.class)));
    }

    @Override
    public Optional<FinanceiroCategoria> buscarCategoria(Long id) {
        return executar("Erro ao buscar categoria", () ->
                jdbc.query(qCategBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(FinanceiroCategoria.class)).stream().findFirst());
    }

    @Override
    public Long inserirCategoria(FinanceiroCategoria c) {
        return executar("Erro ao salvar categoria", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qCategInserir, new BeanPropertySqlParameterSource(c), keys, new String[]{"id_categoria"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void alterarCategoria(FinanceiroCategoria c) {
        executar("Erro ao alterar categoria", () -> jdbc.update(qCategAlterar, new BeanPropertySqlParameterSource(c)));
    }

    @Override
    public int contarLancamentosDaCategoria(Long idCategoria) {
        return executar("Erro ao consultar lançamentos da categoria", () -> {
            Integer total = jdbc.queryForObject(qCategContar, new MapSqlParameterSource("id", idCategoria), Integer.class);
            return total == null ? 0 : total;
        });
    }

    // ---------- lançamentos ----------

    @Override
    public List<FinanceiroLancamento> listarLancamentos(FinanceiroFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancListar + condicoes(filtro, params) + ORDEM_LANCAMENTOS;
        return executar("Erro ao listar lançamentos", () ->
                jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(FinanceiroLancamento.class)));
    }

    @Override
    public Optional<FinanceiroLancamento> buscarLancamento(Long id) {
        return executar("Erro ao buscar lançamento", () ->
                jdbc.query(qLancBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(FinanceiroLancamento.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarLancamentoPorRequisicao(String cdRequisicao) {
        return executar("Erro ao buscar lançamento", () ->
                jdbc.queryForList(qLancRequisicao, new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class)
                        .stream().findFirst());
    }

    @Override
    public Optional<Long> inserirLancamento(FinanceiroLancamento l) {
        return executar("Erro ao salvar lançamento", () ->
                jdbc.queryForList(qLancInserir, new MapSqlParameterSource()
                        .addValue("cdRequisicao", l.getCdRequisicao())
                        .addValue("dtCompetencia", Date.valueOf(l.getDtCompetencia()))
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idCategoria", l.getIdCategoria())
                        .addValue("idPessoa", l.getIdPessoa())
                        .addValue("dsLancamento", l.getDsLancamento())
                        .addValue("vlLancamento", l.getVlLancamento())
                        .addValue("inRealizado", Boolean.TRUE.equals(l.getInRealizado()))
                        .addValue("nrParcela", l.getNrParcela())
                        .addValue("qtParcela", l.getQtParcela())
                        .addValue("idUsuarioCriacao", l.getIdUsuarioCriacao())
                        .addValue("idRecorrencia", l.getIdRecorrencia()), Long.class)
                        .stream().findFirst());
    }

    @Override
    public int alterarLancamento(FinanceiroLancamento l, int nrVersao) {
        return executar("Erro ao alterar lançamento", () ->
                jdbc.update(qLancAlterar, new MapSqlParameterSource()
                        .addValue("id", l.getId())
                        .addValue("dtCompetencia", Date.valueOf(l.getDtCompetencia()))
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idCategoria", l.getIdCategoria())
                        .addValue("idPessoa", l.getIdPessoa())
                        .addValue("dsLancamento", l.getDsLancamento())
                        .addValue("vlLancamento", l.getVlLancamento())
                        .addValue("inRealizado", Boolean.TRUE.equals(l.getInRealizado()))
                        .addValue("nrVersao", nrVersao)));
    }

    @Override
    public int marcarRealizado(Long id, boolean realizado, int nrVersao) {
        return executar("Erro ao atualizar lançamento", () ->
                jdbc.update(qLancRealizado, new MapSqlParameterSource().addValue("id", id)
                        .addValue("inRealizado", realizado).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int deletarLancamento(Long id, int nrVersao) {
        return executar("Erro ao excluir lançamento", () ->
                jdbc.update(qLancDeletar, new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Totais resumir(FinanceiroFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancResumo + condicoes(filtro, params);
        return executar("Erro ao calcular o resumo do Financeiro", () ->
                jdbc.queryForObject(sql, params, (rs, i) -> new Totais(
                        rs.getBigDecimal("entradas"), rs.getBigDecimal("saidas"),
                        rs.getBigDecimal("entradasRealizadas"), rs.getBigDecimal("saidasRealizadas"),
                        rs.getInt("qtLancamentos"), rs.getInt("qtRealizados"))));
    }

    /** Condições AND dos filtros; valores sempre por parâmetro nomeado (sem concatenar texto do cliente). */
    static String condicoes(FinanceiroFiltro filtro, MapSqlParameterSource params) {
        if (filtro == null) return "";
        StringBuilder sb = new StringBuilder();
        if (filtro.competenciaData() != null) {
            sb.append(" AND l.dt_competencia = :competencia");
            params.addValue("competencia", Date.valueOf(filtro.competenciaData()));
        }
        if (filtro.getDe() != null) {
            sb.append(" AND l.dt_lancamento >= :de");
            params.addValue("de", Date.valueOf(filtro.getDe()));
        }
        if (filtro.getAte() != null) {
            sb.append(" AND l.dt_lancamento <= :ate");
            params.addValue("ate", Date.valueOf(filtro.getAte()));
        }
        if (filtro.getIdPessoa() != null) {
            sb.append(" AND l.id_pessoa = :idPessoa");
            params.addValue("idPessoa", filtro.getIdPessoa());
        }
        if (filtro.getIdCategoria() != null) {
            sb.append(" AND l.id_categoria = :idCategoria");
            params.addValue("idCategoria", filtro.getIdCategoria());
        }
        if (filtro.getTipo() != null && !filtro.getTipo().trim().isEmpty()) {
            sb.append(" AND c.cd_tipo = :tipo");
            params.addValue("tipo", filtro.getTipo().trim().toUpperCase());
        }
        String situacao = filtro.getSituacao() == null ? "TODOS" : filtro.getSituacao().trim().toUpperCase();
        if ("REALIZADO".equals(situacao)) sb.append(" AND l.in_realizado = TRUE");
        else if ("PREVISTO".equals(situacao)) sb.append(" AND l.in_realizado = FALSE");
        if (filtro.getTexto() != null && !filtro.getTexto().trim().isEmpty()) {
            sb.append(" AND (l.ds_lancamento ILIKE :texto OR c.nm_categoria ILIKE :texto OR p.nm_pessoa ILIKE :texto)");
            params.addValue("texto", "%" + escaparLike(filtro.getTexto().trim()) + "%");
        }
        return sb.toString();
    }

    /** Barra, % e _ do texto digitado valem como letras (a barra invertida é o escape padrão do LIKE no PostgreSQL). */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
