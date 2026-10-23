package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.DaviJobRankingDto;
import br.com.nord_tool_backend.repository.DaviJobRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DaviJobRepositoryImpl implements DaviJobRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public DaviJobRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.DAVI_JOB.RANKING");
    }

    @Override
    public void inserir(Long idUsuario, int nrPontos) {
        executor.executar("Erro ao salvar pontuação", () -> {
            jdbc.update(queries.get("SPI.DAVI_JOB.PONTUACAO"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("nrPontos", nrPontos));
        });
    }

    @Override
    public List<DaviJobRankingDto> ranking(Long idUsuarioLogado, int limite) {
        return executor.executar("Erro ao carregar o ranking", () ->
                jdbc.query(queries.get("SPS.DAVI_JOB.RANKING"),
                        new MapSqlParameterSource("idUsuario", idUsuarioLogado).addValue("limite", limite).addValue("somenteUsuario", null, java.sql.Types.BIGINT),
                        BeanPropertyRowMapper.newInstance(DaviJobRankingDto.class)));
    }

    @Override
    public Optional<DaviJobRankingDto> recorde(Long idUsuario) {
        return executor.executar("Erro ao carregar o recorde", () ->
                jdbc.query(queries.get("SPS.DAVI_JOB.RANKING"),
                        new MapSqlParameterSource("idUsuario", idUsuario).addValue("limite", 1).addValue("somenteUsuario", idUsuario, java.sql.Types.BIGINT),
                        BeanPropertyRowMapper.newInstance(DaviJobRankingDto.class)).stream().findFirst());
    }
}
