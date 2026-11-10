package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;
import br.com.nord_tool_backend.repository.BlocoNotasRepository;
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
public class BlocoNotasRepositoryImpl implements BlocoNotasRepository {

    private static final BeanPropertyRowMapper<BlocoCadernoDto> CADERNO = BeanPropertyRowMapper.newInstance(BlocoCadernoDto.class);
    private static final BeanPropertyRowMapper<BlocoPaginaDto> PAGINA = BeanPropertyRowMapper.newInstance(BlocoPaginaDto.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public BlocoNotasRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        // Falha cedo se alguma query faltar no catálogo.
        for (String chave : new String[]{"SPS.BLOCO.CADERNOS", "SPS.BLOCO.PAGINAS", "SPU.BLOCO.PAGINA.CONTEUDO", "SPS.BLOCO.IMAGEM.ARQUIVO"}) {
            queries.get(chave);
        }
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    @Override
    public List<BlocoCadernoDto> cadernos(Long idUsuario) {
        return executor.executar("Erro ao listar cadernos", () ->
                jdbc.query(q("SPS.BLOCO.CADERNOS"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", null, Types.BIGINT), CADERNO));
    }

    @Override
    public Optional<BlocoCadernoDto> caderno(Long id, Long idUsuario) {
        return executor.executar("Erro ao buscar caderno", () ->
                jdbc.query(q("SPS.BLOCO.CADERNOS"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", id, Types.BIGINT), CADERNO)
                        .stream().findFirst());
    }

    @Override
    public int contarCadernos(Long idUsuario) {
        return executor.executar("Erro ao contar cadernos", () -> {
            Integer n = jdbc.queryForObject(q("SPS.BLOCO.CADERNOS.CONTAR"), new MapSqlParameterSource("idUsuario", idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public Long inserirCaderno(Long idUsuario, String titulo, String cor) {
        return executor.executar("Erro ao criar caderno", () ->
                jdbc.queryForObject(q("SPI.BLOCO.CADERNO"), new MapSqlParameterSource("idUsuario", idUsuario)
                        .addValue("titulo", titulo).addValue("cor", cor), Long.class));
    }

    @Override
    public int alterarCaderno(Long id, Long idUsuario, String titulo, String cor, int nrVersao) {
        return executor.executar("Erro ao alterar caderno", () ->
                jdbc.update(q("SPU.BLOCO.CADERNO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)
                        .addValue("titulo", titulo).addValue("cor", cor).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void tocarCaderno(Long id) {
        executor.executar("Erro ao atualizar caderno", () -> jdbc.update(q("SPU.BLOCO.CADERNO.TOCAR"), new MapSqlParameterSource("id", id)));
    }

    @Override
    public int excluirCaderno(Long id, Long idUsuario) {
        return executor.executar("Erro ao excluir caderno", () ->
                jdbc.update(q("SPD.BLOCO.CADERNO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario)));
    }

    @Override
    public List<Long> arquivosDoCaderno(Long id) {
        return executor.executar("Erro ao listar arquivos do caderno", () ->
                jdbc.queryForList(q("SPS.BLOCO.CADERNO.ARQUIVOS"), new MapSqlParameterSource("id", id), Long.class));
    }

    @Override
    public List<BlocoPaginaDto> paginas(Long idCaderno, Long idUsuario) {
        return executor.executar("Erro ao listar páginas", () ->
                jdbc.query(q("SPS.BLOCO.PAGINAS"), new MapSqlParameterSource("idUsuario", idUsuario)
                        .addValue("idCaderno", idCaderno, Types.BIGINT).addValue("id", null, Types.BIGINT), PAGINA));
    }

    @Override
    public Optional<BlocoPaginaDto> pagina(Long id, Long idUsuario) {
        return executor.executar("Erro ao buscar página", () ->
                jdbc.query(q("SPS.BLOCO.PAGINAS"), new MapSqlParameterSource("idUsuario", idUsuario)
                        .addValue("idCaderno", null, Types.BIGINT).addValue("id", id, Types.BIGINT), PAGINA).stream().findFirst());
    }

    @Override
    public Optional<String> conteudo(Long id, Long idUsuario) {
        return executor.executar("Erro ao abrir página", () ->
                jdbc.queryForList(q("SPS.BLOCO.PAGINA.CONTEUDO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario), String.class)
                        .stream().findFirst());
    }

    @Override
    public Long inserirPagina(Long idCaderno, int ordem, String fundo, int largura, int altura, Long idArquivo, String conteudo) {
        return executor.executar("Erro ao criar página", () -> {
            jdbc.update(q("SPU.BLOCO.PAGINAS.EMPURRAR"), new MapSqlParameterSource("idCaderno", idCaderno).addValue("ordem", ordem));
            return jdbc.queryForObject(q("SPI.BLOCO.PAGINA"), new MapSqlParameterSource("idCaderno", idCaderno).addValue("ordem", ordem)
                    .addValue("fundo", fundo).addValue("largura", largura).addValue("altura", altura)
                    .addValue("idArquivo", idArquivo, Types.BIGINT).addValue("conteudo", conteudo), Long.class);
        });
    }

    @Override
    public void reordenar(Long idCaderno, List<Long> ids) {
        executor.executar("Erro ao reordenar páginas", () -> {
            int ordem = 1;
            for (Long id : ids) {
                jdbc.update(q("SPU.BLOCO.PAGINA.ORDEM"), new MapSqlParameterSource("id", id).addValue("idCaderno", idCaderno).addValue("ordem", ordem++));
            }
        });
    }

    @Override
    public int salvarConteudo(Long id, String conteudo, String fundo, int nrVersao) {
        return executor.executar("Erro ao salvar página", () ->
                jdbc.update(q("SPU.BLOCO.PAGINA.CONTEUDO"), new MapSqlParameterSource("id", id).addValue("conteudo", conteudo)
                        .addValue("fundo", fundo).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void excluirPagina(Long id) {
        executor.executar("Erro ao excluir página", () -> jdbc.update(q("SPD.BLOCO.PAGINA"), new MapSqlParameterSource("id", id)));
    }

    @Override
    public Long inserirImagem(Long idCaderno, Long idArquivo) {
        return executor.executar("Erro ao salvar imagem", () ->
                jdbc.queryForObject(q("SPI.BLOCO.IMAGEM"), new MapSqlParameterSource("idCaderno", idCaderno).addValue("idArquivo", idArquivo), Long.class));
    }

    @Override
    public Optional<Long> arquivoDaImagem(Long id, Long idUsuario) {
        return executor.executar("Erro ao buscar imagem", () ->
                jdbc.queryForList(q("SPS.BLOCO.IMAGEM.ARQUIVO"), new MapSqlParameterSource("id", id).addValue("idUsuario", idUsuario), Long.class)
                        .stream().findFirst());
    }

    // ---------- v2 ----------

    @Override
    public void girarPagina(Long id, int rotacao, int largura, int altura) {
        executor.executar("Erro ao girar página", () -> jdbc.update(q("SPU.BLOCO.PAGINA.GIRAR"), new MapSqlParameterSource("id", id)
                .addValue("rotacao", rotacao).addValue("largura", largura).addValue("altura", altura)));
    }

    @Override
    public Optional<br.com.nord_tool_backend.dto.BlocoDocumentoDto> documento(Long idCaderno, Long idUsuario) {
        return executor.executar("Erro ao abrir o documento", () ->
                jdbc.query(q("SPS.BLOCO.DOCUMENTO"), new MapSqlParameterSource("id", idCaderno).addValue("idUsuario", idUsuario),
                        BeanPropertyRowMapper.newInstance(br.com.nord_tool_backend.dto.BlocoDocumentoDto.class)).stream().findFirst());
    }

    @Override
    public int salvarDocumento(Long idCaderno, Long idUsuario, String html, int nrVersao) {
        return executor.executar("Erro ao salvar o documento", () ->
                jdbc.update(q("SPU.BLOCO.DOCUMENTO"), new MapSqlParameterSource("id", idCaderno).addValue("idUsuario", idUsuario)
                        .addValue("html", html).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Long inserirPdf(Long idCaderno, Long idArquivo) {
        return executor.executar("Erro ao guardar o PDF", () ->
                jdbc.queryForObject(q("SPI.BLOCO.PDF"), new MapSqlParameterSource("idCaderno", idCaderno).addValue("idArquivo", idArquivo), Long.class));
    }

    @Override
    public Optional<Long> arquivoDoPdf(Long idPdf, Long idUsuario, Long idCaderno) {
        return executor.executar("Erro ao buscar o PDF", () ->
                jdbc.queryForList(q("SPS.BLOCO.PDF"), new MapSqlParameterSource("id", idPdf).addValue("idUsuario", idUsuario)
                        .addValue("idCaderno", idCaderno, Types.BIGINT), Long.class).stream().findFirst());
    }

    @Override
    public void ligarPdf(Long idPagina, Long idPdf, Integer nrPagina) {
        executor.executar("Erro ao ligar a página ao PDF", () -> jdbc.update(q("SPU.BLOCO.PAGINA.PDF"), new MapSqlParameterSource("id", idPagina)
                .addValue("idPdf", idPdf, Types.BIGINT).addValue("nrPagina", nrPagina, Types.INTEGER)));
    }
}
