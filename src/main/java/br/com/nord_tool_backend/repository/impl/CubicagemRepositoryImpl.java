package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.repository.CubicagemRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação (ou entra na do serviço). */
@Repository
@Transactional
public class CubicagemRepositoryImpl implements CubicagemRepository {

    private static final BeanPropertyRowMapper<CubicagemDto> LINHA = BeanPropertyRowMapper.newInstance(CubicagemDto.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public CubicagemRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String chave : new String[]{"SPS.CUBICAGENS", "SPS.CUBICAGEM", "SPI.CUBICAGEM", "SPU.CUBICAGEM", "SPD.CUBICAGEM"}) {
            queries.get(chave);
        }
    }

    @Override
    public List<CubicagemDto> listar(Long idUsuario) {
        return executor.executar("Erro ao listar cubicagens", () ->
                jdbc.query(queries.get("SPS.CUBICAGENS"), new MapSqlParameterSource("idUsuario", idUsuario), LINHA));
    }

    @Override
    public Optional<CubicagemDto> buscar(Long id, Long idUsuario) {
        return executor.executar("Erro ao buscar cubicagem", () ->
                jdbc.query(queries.get("SPS.CUBICAGEM"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario), LINHA)
                        .stream().findFirst());
    }

    @Override
    public int contar(Long idUsuario) {
        return executor.executar("Erro ao contar cubicagens", () -> {
            Integer n = jdbc.queryForObject(queries.get("SPS.CUBICAGENS.CONTAR"), new MapSqlParameterSource("idUsuario", idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public Long inserir(Long idUsuario, String titulo, BigDecimal perda, String pecas) {
        return executor.executar("Erro ao criar cubicagem", () ->
                jdbc.queryForObject(queries.get("SPI.CUBICAGEM"), new MapSqlParameterSource("idUsuario", idUsuario)
                        .addValue("titulo", titulo).addValue("perda", perda, Types.NUMERIC).addValue("pecas", pecas), Long.class));
    }

    @Override
    public int alterar(Long id, Long idUsuario, String titulo, BigDecimal perda, String pecas, int nrVersao) {
        return executor.executar("Erro ao salvar cubicagem", () ->
                jdbc.update(queries.get("SPU.CUBICAGEM"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)
                        .addValue("titulo", titulo).addValue("perda", perda, Types.NUMERIC).addValue("pecas", pecas)
                        .addValue("nrVersao", nrVersao)));
    }

    @Override
    public int excluir(Long id, Long idUsuario) {
        return executor.executar("Erro ao excluir cubicagem", () ->
                jdbc.update(queries.get("SPD.CUBICAGEM"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)));
    }
}
