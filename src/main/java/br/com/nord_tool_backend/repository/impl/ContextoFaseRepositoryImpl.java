package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.repository.ContextoFaseRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
public class ContextoFaseRepositoryImpl implements ContextoFaseRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries q;

    public ContextoFaseRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.q = queries;
        for (String chave : new String[]{"SPS.CONTEXTO.EXISTE_FASE", "SPS.CONTEXTO.FASES", "SPS.CONTEXTO.FASES_USUARIO", "SPS.CONTEXTO.OBRAS"}) {
            queries.get(chave);
        }
    }

    @Override
    public boolean existeFase() {
        return executor.executar("Conferir fases", () -> Boolean.TRUE.equals(
                jdbc.queryForObject(q.get("SPS.CONTEXTO.EXISTE_FASE"), EmptySqlParameterSource.INSTANCE, Boolean.class)));
    }

    @Override
    public List<Fase> fasesAtivas(Long idObra) {
        return executor.executar("Listar fases", () -> jdbc.query(q.get("SPS.CONTEXTO.FASES"),
                new MapSqlParameterSource().addValue("idObra", idObra, Types.BIGINT), BeanPropertyRowMapper.newInstance(Fase.class)));
    }

    @Override
    public List<Fase> fasesDoUsuario(Long idUsuario, Long idObra) {
        return executor.executar("Listar fases do usuário", () -> jdbc.query(q.get("SPS.CONTEXTO.FASES_USUARIO"),
                new MapSqlParameterSource().addValue("idUsuario", idUsuario).addValue("idObra", idObra, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(Fase.class)));
    }

    @Override
    public List<Obra> obrasAtivas() {
        return executor.executar("Listar obras", () -> jdbc.query(q.get("SPS.CONTEXTO.OBRAS"), BeanPropertyRowMapper.newInstance(Obra.class)));
    }
}
