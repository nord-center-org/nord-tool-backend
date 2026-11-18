package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.AppAparelhoDto;
import br.com.nord_tool_backend.repository.AppAparelhoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class AppAparelhoRepositoryImpl implements AppAparelhoRepository {

    private static final BeanPropertyRowMapper<AppAparelhoDto> APARELHO = BeanPropertyRowMapper.newInstance(AppAparelhoDto.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public AppAparelhoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.APP_APARELHO.POR_HASH");
    }

    @Override
    public void criar(Long idUsuario, String hash, String aparelho, int versaoSessao) {
        executor.executar("Erro ao registrar o aparelho", () -> jdbc.update(queries.get("SPI.APP_APARELHO.CRIAR"),
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("hash", hash).addValue("aparelho", aparelho).addValue("versao", versaoSessao)));
    }

    @Override
    public Optional<AppAparelhoDto> porHash(String hash, int dias) {
        return executor.executar("Erro ao buscar o aparelho", () -> jdbc.query(queries.get("SPS.APP_APARELHO.POR_HASH"),
                new MapSqlParameterSource("hash", hash).addValue("dias", dias), APARELHO).stream().findFirst());
    }

    @Override
    public void marcarUso(Long id) {
        executor.executar("Erro ao registrar o uso do aparelho", () -> jdbc.update(queries.get("SPU.APP_APARELHO.USO"), new MapSqlParameterSource("id", id)));
    }

    @Override
    public int revogar(String hash) {
        return executor.executar("Erro ao desconectar o aparelho", () -> jdbc.update(queries.get("SPU.APP_APARELHO.REVOGAR"), new MapSqlParameterSource("hash", hash)));
    }

    @Override
    public int limitar(Long idUsuario, int limite) {
        return executor.executar("Erro ao limitar os aparelhos", () -> jdbc.update(queries.get("SPU.APP_APARELHO.EXCEDENTES"),
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("limite", limite)));
    }
}
