package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.repository.MenuFavoritoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class MenuFavoritoRepositoryImpl implements MenuFavoritoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public MenuFavoritoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.MENU.FAVORITOS");
        queries.get("SPU.MENU.FAVORITOS");
    }

    @Override
    public Optional<String> buscar(Long idUsuario) {
        return executor.executar("Erro ao ler os favoritos do menu", () ->
                jdbc.queryForList(queries.get("SPS.MENU.FAVORITOS"), new MapSqlParameterSource("idUsuario", idUsuario), String.class)
                        .stream().findFirst());
    }

    @Override
    public void salvar(Long idUsuario, String caminhos) {
        executor.executar("Erro ao salvar os favoritos do menu", () ->
                jdbc.update(queries.get("SPU.MENU.FAVORITOS"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("caminhos", caminhos)));
    }
}
