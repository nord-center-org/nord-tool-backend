package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.CasamentoArteDto;
import br.com.nord_tool_backend.repository.CasamentoArteRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class CasamentoArteRepositoryImpl implements CasamentoArteRepository {

    private static final String ID = "id";
    private static final String TITULO = "titulo";
    private static final String CATEGORIA = "categoria";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public CasamentoArteRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String q : new String[]{"SPS.CASAMENTO.ARTE.LISTAR", "SPS.CASAMENTO.ARTE.BUSCAR", "SPS.CASAMENTO.ARTE.CONTAR",
                "SPI.CASAMENTO.ARTE.INSERIR", "SPU.CASAMENTO.ARTE.ATUALIZAR", "SPD.CASAMENTO.ARTE.DELETAR"}) {
            queries.get(q);
        }
    }

    @Override
    public List<CasamentoArteDto> listar() {
        return executor.executar("Erro ao listar as artes", () -> jdbc.query(queries.get("SPS.CASAMENTO.ARTE.LISTAR"),
                new MapSqlParameterSource(), BeanPropertyRowMapper.newInstance(CasamentoArteDto.class)));
    }

    @Override
    public Optional<CasamentoArteDto> buscar(Long id) {
        List<CasamentoArteDto> r = executor.executar("Erro ao buscar a arte", () -> jdbc.query(queries.get("SPS.CASAMENTO.ARTE.BUSCAR"),
                new MapSqlParameterSource(ID, id), BeanPropertyRowMapper.newInstance(CasamentoArteDto.class)));
        return r.stream().findFirst();
    }

    @Override
    public long contar() {
        Long n = executor.executar("Erro ao contar as artes", () -> jdbc.queryForObject(queries.get("SPS.CASAMENTO.ARTE.CONTAR"),
                new MapSqlParameterSource(), Long.class));
        return n == null ? 0 : n;
    }

    @Override
    public Long inserir(Long idArquivo, String titulo, String categoria, Long idUsuario) {
        MapSqlParameterSource p = new MapSqlParameterSource("idArquivo", idArquivo).addValue(TITULO, titulo)
                .addValue(CATEGORIA, categoria).addValue("idUsuario", idUsuario);
        return executor.executar("Erro ao salvar a arte", () -> jdbc.queryForObject(queries.get("SPI.CASAMENTO.ARTE.INSERIR"), p, Long.class));
    }

    @Override
    public void atualizar(Long id, String titulo, String categoria) {
        MapSqlParameterSource p = new MapSqlParameterSource(ID, id).addValue(TITULO, titulo).addValue(CATEGORIA, categoria);
        executor.executar("Erro ao atualizar a arte", () -> jdbc.update(queries.get("SPU.CASAMENTO.ARTE.ATUALIZAR"), p));
    }

    @Override
    public void deletar(Long id) {
        executor.executar("Erro ao excluir a arte", () -> jdbc.update(queries.get("SPD.CASAMENTO.ARTE.DELETAR"), new MapSqlParameterSource(ID, id)));
    }
}
