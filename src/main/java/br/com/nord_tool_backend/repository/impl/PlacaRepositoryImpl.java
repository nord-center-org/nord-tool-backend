package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.repository.PlacaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class PlacaRepositoryImpl implements PlacaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public PlacaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        // Falha cedo se alguma query faltar no catálogo.
        for (String chave : new String[]{"SPS.PLACA.MODELOS", "SPS.PLACA.LISTAR", "SPI.PLACA.INSERIR", "SPU.PLACA.MODELO.LOGO"}) {
            queries.get(chave);
        }
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    private static final BeanPropertyRowMapper<PlacaModeloDto> MODELO = BeanPropertyRowMapper.newInstance(PlacaModeloDto.class);
    private static final BeanPropertyRowMapper<PlacaDto> PLACA = BeanPropertyRowMapper.newInstance(PlacaDto.class);

    @Override
    public List<PlacaModeloDto> modelos() {
        return executor.executar("Erro ao listar modelos de placa", () ->
                jdbc.query(q("SPS.PLACA.MODELOS"), new MapSqlParameterSource("id", null), MODELO));
    }

    @Override
    public Optional<PlacaModeloDto> modelo(Long id) {
        return executor.executar("Erro ao buscar modelo de placa", () ->
                jdbc.query(q("SPS.PLACA.MODELOS"), new MapSqlParameterSource().addValue("id", id, Types.BIGINT), MODELO).stream().findFirst());
    }

    @Override
    public Long inserirModelo(String nome, String config, boolean ativo, Long idUsuario) {
        return executor.executar("Erro ao salvar modelo de placa", () ->
                jdbc.queryForObject(q("SPI.PLACA.MODELO.INSERIR"), new MapSqlParameterSource()
                        .addValue("nome", nome).addValue("config", config).addValue("ativo", ativo).addValue("idUsuario", idUsuario), Long.class));
    }

    @Override
    public int alterarModelo(Long id, String nome, String config, boolean ativo, Long idUsuario) {
        return executor.executar("Erro ao alterar modelo de placa", () ->
                jdbc.update(q("SPU.PLACA.MODELO.ALTERAR"), new MapSqlParameterSource()
                        .addValue("id", id).addValue("nome", nome).addValue("config", config).addValue("ativo", ativo).addValue("idUsuario", idUsuario)));
    }

    @Override
    public int trocarImagem(Long idModelo, boolean fundo, Long idArquivo, Long idUsuario) {
        return executor.executar("Erro ao trocar a imagem do modelo", () ->
                jdbc.update(q(fundo ? "SPU.PLACA.MODELO.FUNDO" : "SPU.PLACA.MODELO.LOGO"), new MapSqlParameterSource()
                        .addValue("id", idModelo).addValue("idArquivo", idArquivo, Types.BIGINT).addValue("idUsuario", idUsuario)));
    }

    @Override
    public int placasDoModelo(Long idModelo) {
        return executor.executar("Erro ao contar placas do modelo", () -> {
            Integer n = jdbc.queryForObject(q("SPS.PLACA.MODELO.USO"), new MapSqlParameterSource("id", idModelo), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public int excluirModelo(Long id) {
        return executor.executar("Erro ao excluir modelo de placa", () ->
                jdbc.update(q("SPD.PLACA.MODELO.EXCLUIR"), new MapSqlParameterSource("id", id)));
    }

    @Override
    public List<PlacaDto> placas(Long idObra) {
        return executor.executar("Erro ao listar placas", () ->
                jdbc.query(q("SPS.PLACA.LISTAR"), new MapSqlParameterSource().addValue("idObra", idObra, Types.BIGINT).addValue("id", null, Types.BIGINT), PLACA));
    }

    @Override
    public Optional<PlacaDto> placa(Long id) {
        return executor.executar("Erro ao buscar placa", () ->
                jdbc.query(q("SPS.PLACA.LISTAR"), new MapSqlParameterSource().addValue("idObra", null, Types.BIGINT).addValue("id", id, Types.BIGINT), PLACA).stream().findFirst());
    }

    @Override
    public Long inserirPlaca(Long idObra, Long idModelo, String titulo, String valores, Long idUsuario) {
        return executor.executar("Erro ao salvar placa", () ->
                jdbc.queryForObject(q("SPI.PLACA.INSERIR"), new MapSqlParameterSource()
                        .addValue("idObra", idObra).addValue("idModelo", idModelo).addValue("titulo", titulo)
                        .addValue("valores", valores).addValue("idUsuario", idUsuario), Long.class));
    }

    @Override
    public int alterarPlaca(Long id, Long idModelo, String titulo, String valores, Long idUsuario) {
        return executor.executar("Erro ao alterar placa", () ->
                jdbc.update(q("SPU.PLACA.ALTERAR"), new MapSqlParameterSource()
                        .addValue("id", id).addValue("idModelo", idModelo).addValue("titulo", titulo)
                        .addValue("valores", valores).addValue("idUsuario", idUsuario)));
    }

    @Override
    public int excluirPlaca(Long id) {
        return executor.executar("Erro ao excluir placa", () ->
                jdbc.update(q("SPD.PLACA.EXCLUIR"), new MapSqlParameterSource("id", id)));
    }
}
