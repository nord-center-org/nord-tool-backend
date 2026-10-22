package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.dto.MapeamentoLinhaDto;
import br.com.nord_tool_backend.repository.MapeamentoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import br.com.nord_tool_backend.service.ContextoFaseService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class MapeamentoRepositoryImpl implements MapeamentoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final ContextoFaseService contexto;
    private final String qListar;
    private final String qLinhas;
    private final String qInserir;
    private final String qAlterar;
    private final String qStatus;
    private final String qDeletar;
    private final String qLimpar;
    private final String qLinhaInserir;

    public MapeamentoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries, ContextoFaseService contexto) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.contexto = contexto;
        this.qListar = queries.get("SPS.MAPEAMENTO.LISTAR");
        this.qLinhas = queries.get("SPS.MAPEAMENTO.LINHAS");
        this.qInserir = queries.get("SPI.MAPEAMENTO.INSERIR");
        this.qAlterar = queries.get("SPU.MAPEAMENTO.ALTERAR");
        this.qStatus = queries.get("SPU.MAPEAMENTO.STATUS");
        this.qDeletar = queries.get("SPD.MAPEAMENTO.DELETAR");
        this.qLimpar = queries.get("SPD.MAPEAMENTO.LINHAS.LIMPAR");
        this.qLinhaInserir = queries.get("SPI.MAPEAMENTO.LINHA.INSERIR");
    }

    @Override
    public List<MapeamentoDto> listar() {
        return executor.executar("Erro ao listar mapeamentos", () ->
                jdbc.query(qListar, contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", null, Types.BIGINT)),
                        BeanPropertyRowMapper.newInstance(MapeamentoDto.class)));
    }

    @Override
    public Optional<MapeamentoDto> buscar(Long id) {
        return executor.executar("Erro ao buscar mapeamento", () ->
                jdbc.query(qListar, contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", id, Types.BIGINT)),
                        BeanPropertyRowMapper.newInstance(MapeamentoDto.class)).stream().findFirst());
    }

    @Override
    public List<MapeamentoLinhaDto> listarLinhas(Long idMapeamento) {
        return executor.executar("Erro ao listar linhas do mapeamento", () ->
                jdbc.query(qLinhas, new MapSqlParameterSource("idMapeamento", idMapeamento), BeanPropertyRowMapper.newInstance(MapeamentoLinhaDto.class)));
    }

    @Override
    public Long inserir(MapeamentoDto m, Long idFase) {
        return executor.executar("Erro ao salvar mapeamento", () ->
                jdbc.queryForObject(qInserir, valores(m).addValue("cdTipo", m.getCdTipo()).addValue("idFase", idFase, Types.BIGINT)
                        .addValue("idAutor", m.getIdAutor()), Long.class));
    }

    @Override
    public int alterar(MapeamentoDto m, int nrVersao) {
        return executor.executar("Erro ao alterar mapeamento", () ->
                jdbc.update(qAlterar, valores(m).addValue("id", m.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int alterarStatus(Long id, String cdStatus, int nrVersao) {
        return executor.executar("Erro ao alterar o status do mapeamento", () ->
                jdbc.update(qStatus, new MapSqlParameterSource().addValue("id", id).addValue("cdStatus", cdStatus).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int deletar(Long id, int nrVersao) {
        return executor.executar("Erro ao excluir mapeamento", () ->
                jdbc.update(qDeletar, new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void substituirLinhas(Long idMapeamento, List<MapeamentoLinhaDto> linhas) {
        executor.executar("Erro ao salvar linhas do mapeamento", () -> {
            jdbc.update(qLimpar, new MapSqlParameterSource("idMapeamento", idMapeamento));
            if (linhas.isEmpty()) return null;
            MapSqlParameterSource[] lote = linhas.stream().map(l -> new MapSqlParameterSource()
                    .addValue("idMapeamento", idMapeamento).addValue("nrOrdem", l.getNrOrdem())
                    .addValue("nmGrupo", l.getNmGrupo()).addValue("nmItem", l.getNmItem()).addValue("nmUnidade", l.getNmUnidade())
                    .addValue("qtProjeto", l.getQtProjeto(), Types.NUMERIC).addValue("qtMedida", l.getQtMedida(), Types.NUMERIC)
                    .addValue("qtSolicitada", l.getQtSolicitada(), Types.NUMERIC).addValue("txObservacao", l.getTxObservacao()))
                    .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate(qLinhaInserir, lote);
            return null;
        });
    }

    private static MapSqlParameterSource valores(MapeamentoDto m) {
        return new MapSqlParameterSource()
                .addValue("nmTitulo", m.getNmTitulo())
                .addValue("nmLocal", m.getNmLocal())
                .addValue("dtMapeamento", Date.valueOf(m.getDtMapeamento()))
                .addValue("pcTolerancia", m.getPcTolerancia())
                .addValue("txObservacao", m.getTxObservacao());
    }
}
