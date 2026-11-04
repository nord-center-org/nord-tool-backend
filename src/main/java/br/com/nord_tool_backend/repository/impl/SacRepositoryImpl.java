package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.dto.SacImagemDto;
import br.com.nord_tool_backend.repository.SacRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class SacRepositoryImpl implements SacRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public SacRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        // Falha cedo se alguma query faltar no catálogo.
        for (String chave : new String[]{"SPS.SAC.LISTAR", "SPS.SAC.IMAGENS", "SPI.SAC.INSERIR", "SPU.SAC.RESPONDER"}) {
            queries.get(chave);
        }
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    @Override
    public List<SacChamadoDto> listar(Long idUsuario) {
        return executor.executar("Erro ao listar chamados do SAC", () ->
                jdbc.query(q("SPS.SAC.LISTAR"), new MapSqlParameterSource().addValue("idUsuario", idUsuario, Types.BIGINT).addValue("id", null, Types.BIGINT),
                        BeanPropertyRowMapper.newInstance(SacChamadoDto.class)));
    }

    @Override
    public Optional<SacChamadoDto> buscar(Long id) {
        return executor.executar("Erro ao buscar chamado do SAC", () ->
                jdbc.query(q("SPS.SAC.LISTAR"), new MapSqlParameterSource().addValue("idUsuario", null, Types.BIGINT).addValue("id", id, Types.BIGINT),
                        BeanPropertyRowMapper.newInstance(SacChamadoDto.class)).stream().findFirst());
    }

    @Override
    public List<SacImagemDto> imagens(List<Long> idsChamados) {
        if (idsChamados.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao listar imagens do SAC", () ->
                jdbc.query(q("SPS.SAC.IMAGENS"), new MapSqlParameterSource("ids", idsChamados), BeanPropertyRowMapper.newInstance(SacImagemDto.class)));
    }

    @Override
    public Optional<SacImagemDto> imagem(Long idImagem) {
        return executor.executar("Erro ao buscar imagem do SAC", () ->
                jdbc.query(q("SPS.SAC.IMAGEM"), new MapSqlParameterSource("id", idImagem), BeanPropertyRowMapper.newInstance(SacImagemDto.class)).stream().findFirst());
    }

    @Override
    public Long inserir(Long idUsuario, String nmTitulo, String txDescricao, String txTela) {
        return executor.executar("Erro ao abrir chamado do SAC", () ->
                jdbc.queryForObject(q("SPI.SAC.INSERIR"), new MapSqlParameterSource()
                        .addValue("idUsuario", idUsuario).addValue("nmTitulo", nmTitulo)
                        .addValue("txDescricao", txDescricao).addValue("txTela", txTela, Types.VARCHAR), Long.class));
    }

    @Override
    public void inserirImagem(Long idChamado, Long idArquivo, int nrOrdem) {
        executor.executar("Erro ao salvar imagem do SAC", () ->
                jdbc.update(q("SPI.SAC.IMAGEM.INSERIR"), new MapSqlParameterSource()
                        .addValue("idChamado", idChamado).addValue("idArquivo", idArquivo).addValue("nrOrdem", nrOrdem)));
    }

    @Override
    public int responder(Long id, String cdStatus, String txResposta, Long idUsuarioResposta) {
        return executor.executar("Erro ao responder chamado do SAC", () ->
                jdbc.update(q("SPU.SAC.RESPONDER"), new MapSqlParameterSource()
                        .addValue("id", id).addValue("cdStatus", cdStatus)
                        .addValue("txResposta", txResposta, Types.VARCHAR).addValue("idUsuarioResposta", idUsuarioResposta)));
    }

    @Override
    public List<Long> arquivos(Long idChamado) {
        return executor.executar("Erro ao listar arquivos do chamado", () ->
                jdbc.queryForList(q("SPS.SAC.ARQUIVOS"), new MapSqlParameterSource("idChamado", idChamado), Long.class));
    }

    @Override
    public int excluir(Long id) {
        return executor.executar("Erro ao excluir chamado do SAC", () ->
                jdbc.update(q("SPD.SAC.EXCLUIR"), new MapSqlParameterSource("id", id)));
    }
}
