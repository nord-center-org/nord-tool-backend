package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.BlocoTrechoDto;
import br.com.nord_tool_backend.repository.BlocoAssistenteRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** O pool roda com auto-commit desligado: cada leitura tem a própria transação (somente leitura). */
@Repository
@Transactional(readOnly = true)
public class BlocoAssistenteRepositoryImpl implements BlocoAssistenteRepository {

    private static final BeanPropertyRowMapper<BlocoTrechoDto> LINHA = BeanPropertyRowMapper.newInstance(BlocoTrechoDto.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public BlocoAssistenteRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.BLOCO.ASSISTENTE.CATALOGO");
        queries.get("SPS.BLOCO.ASSISTENTE.TRECHOS");
    }

    @Override
    public List<BlocoTrechoDto> catalogo(Long idUsuario, int limite) {
        return executor.executar("Erro ao listar o Bloco de notas", () ->
                jdbc.query(queries.get("SPS.BLOCO.ASSISTENTE.CATALOGO"), parametros(idUsuario, limite), LINHA));
    }

    @Override
    public List<BlocoTrechoDto> trechos(Long idUsuario, int limite) {
        return executor.executar("Erro ao ler o Bloco de notas", () ->
                jdbc.query(queries.get("SPS.BLOCO.ASSISTENTE.TRECHOS"), parametros(idUsuario, limite), LINHA));
    }

    private static MapSqlParameterSource parametros(Long idUsuario, int limite) {
        return new MapSqlParameterSource("idUsuario", idUsuario).addValue("limite", limite);
    }
}
