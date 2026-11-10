package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.repository.BlocoArquivoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação (ou entra na do serviço). */
@Repository
@Transactional
public class BlocoArquivoRepositoryImpl implements BlocoArquivoRepository {

    private static final BeanPropertyRowMapper<BlocoArquivoDto> LINHA = BeanPropertyRowMapper.newInstance(BlocoArquivoDto.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public BlocoArquivoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String chave : new String[]{"SPS.BLOCO.ARQUIVOS", "SPS.BLOCO.ARQUIVO", "SPI.BLOCO.ARQUIVO", "SPU.BLOCO.ARQUIVO", "SPD.BLOCO.ARQUIVO"}) {
            queries.get(chave);
        }
    }

    @Override
    public List<BlocoArquivoDto> listar(Long idUsuario) {
        return executor.executar("Erro ao listar planilhas e documentos", () ->
                jdbc.query(queries.get("SPS.BLOCO.ARQUIVOS"), new MapSqlParameterSource("idUsuario", idUsuario), LINHA));
    }

    @Override
    public Optional<BlocoArquivoDto> buscar(Long id, Long idUsuario) {
        return executor.executar("Erro ao abrir o arquivo", () ->
                jdbc.query(queries.get("SPS.BLOCO.ARQUIVO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario), LINHA)
                        .stream().findFirst());
    }

    @Override
    public int contar(Long idUsuario) {
        return executor.executar("Erro ao contar arquivos", () -> {
            Integer n = jdbc.queryForObject(queries.get("SPS.BLOCO.ARQUIVOS.CONTAR"), new MapSqlParameterSource("idUsuario", idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public Long inserir(Long idUsuario, String tipo, String titulo, String cor, String conteudo) {
        return executor.executar("Erro ao criar o arquivo", () ->
                jdbc.queryForObject(queries.get("SPI.BLOCO.ARQUIVO"), new MapSqlParameterSource("idUsuario", idUsuario)
                        .addValue("tipo", tipo).addValue("titulo", titulo).addValue("cor", cor).addValue("conteudo", conteudo), Long.class));
    }

    @Override
    public int alterar(Long id, Long idUsuario, String titulo, String cor, String conteudo, int nrVersao) {
        return executor.executar("Erro ao salvar o arquivo", () ->
                jdbc.update(queries.get("SPU.BLOCO.ARQUIVO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)
                        .addValue("titulo", titulo).addValue("cor", cor).addValue("conteudo", conteudo, Types.VARCHAR)
                        .addValue("nrVersao", nrVersao)));
    }

    @Override
    public int excluir(Long id, Long idUsuario) {
        return executor.executar("Erro ao excluir o arquivo", () ->
                jdbc.update(queries.get("SPD.BLOCO.ARQUIVO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)));
    }
}
