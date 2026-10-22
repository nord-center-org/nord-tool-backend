package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.repository.AvatarRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AvatarRepositoryImpl implements AvatarRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public AvatarRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.AVATAR.BUSCAR");
    }

    @Override
    public Optional<AvatarDto> buscar(Long idUsuario) {
        return executor.executar("Erro ao buscar avatar", () ->
                jdbc.query(queries.get("SPS.AVATAR.BUSCAR"), new MapSqlParameterSource("idUsuario", idUsuario),
                        BeanPropertyRowMapper.newInstance(AvatarDto.class)).stream().findFirst());
    }

    @Override
    public void salvar(Long idUsuario, String dsConfig) {
        executor.executar("Erro ao salvar avatar", () -> {
            jdbc.update(queries.get("SPI.AVATAR.SALVAR"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("dsConfig", dsConfig));
        });
    }

    @Override
    public List<AvatarDto> equipe(Long idUsuario, int limite) {
        return executor.executar("Erro ao listar avatares da equipe", () ->
                jdbc.query(queries.get("SPS.AVATAR.EQUIPE"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("limite", limite),
                        BeanPropertyRowMapper.newInstance(AvatarDto.class)));
    }
}
