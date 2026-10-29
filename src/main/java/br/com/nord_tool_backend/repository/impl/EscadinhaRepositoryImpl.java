package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.EscadinhaTorre;
import br.com.nord_tool_backend.repository.EscadinhaRepository;
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

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class EscadinhaRepositoryImpl implements EscadinhaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qTorres;
    private final String qContar;
    private final String qInserir;
    private final String qAlterar;
    private final String qExcluir;
    private final String qPlanta;
    private final String qModelo;
    private final String qGravarModelo;
    private final SqlQueries q;

    public EscadinhaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qTorres = queries.get("SPS.ESCADINHA.TORRES");
        this.qContar = queries.get("SPS.ESCADINHA.TORRES.CONTAR");
        this.qInserir = queries.get("SPI.ESCADINHA.TORRE");
        this.qAlterar = queries.get("SPU.ESCADINHA.TORRE");
        this.qExcluir = queries.get("SPD.ESCADINHA.TORRE");
        this.qPlanta = queries.get("SPS.ESCADINHA.TORRE.PLANTA");
        this.qModelo = queries.get("SPS.ESCADINHA.MODELO");
        this.qGravarModelo = queries.get("SPI.ESCADINHA.MODELO");
        this.q = queries;
    }

    @Override
    public List<EscadinhaTorre> listarTorres(Long idObra) {
        return executor.executar("Erro ao listar as torres da escadinha", () -> jdbc.query(qTorres,
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", null, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(EscadinhaTorre.class)));
    }

    @Override
    public Optional<EscadinhaTorre> buscarTorre(Long idObra, Long idTorre) {
        return executor.executar("Erro ao buscar a torre da escadinha", () -> jdbc.query(qTorres,
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(EscadinhaTorre.class)).stream().findFirst());
    }

    @Override
    public int contarTorres(Long idObra) {
        return executor.executar("Erro ao contar as torres da escadinha", () -> {
            Integer n = jdbc.queryForObject(qContar, new MapSqlParameterSource("idObra", idObra), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public Long inserirTorre(Long idObra, String nome, int ordem, String config, String ciclos, String planta, Long idUsuario) {
        return executor.executar("Erro ao criar a torre da escadinha", () -> jdbc.queryForObject(qInserir,
                new MapSqlParameterSource("idObra", idObra).addValue("nome", nome).addValue("ordem", ordem)
                        .addValue("config", config).addValue("ciclos", ciclos).addValue("planta", planta, Types.VARCHAR)
                        .addValue("idUsuario", idUsuario), Long.class));
    }

    @Override
    public int alterarTorre(Long idObra, Long idTorre, int versao, String nome, int ordem, String config, String ciclos,
                            boolean mudarPlanta, String planta, Long idUsuario) {
        return executor.executar("Erro ao salvar a torre da escadinha", () -> jdbc.update(qAlterar,
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre).addValue("versao", versao)
                        .addValue("nome", nome).addValue("ordem", ordem).addValue("config", config).addValue("ciclos", ciclos)
                        .addValue("mudarPlanta", mudarPlanta).addValue("planta", planta, Types.VARCHAR)
                        .addValue("idUsuario", idUsuario)));
    }

    @Override
    public int excluirTorre(Long idObra, Long idTorre) {
        return executor.executar("Erro ao excluir a torre da escadinha", () -> jdbc.update(qExcluir,
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre)));
    }

    @Override
    public Optional<String> planta(Long idObra, Long idTorre) {
        return executor.executar("Erro ao ler a prancha da torre", () -> jdbc.query(qPlanta,
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre),
                (rs, i) -> rs.getString("planta")).stream().findFirst());
    }

    @Override
    public Optional<String> modelo(Long idObra) {
        return executor.executar("Erro ao ler o modelo de ciclos", () -> jdbc.query(qModelo,
                new MapSqlParameterSource("idObra", idObra), (rs, i) -> rs.getString("modelo")).stream().findFirst());
    }

    @Override
    public void gravarModelo(Long idObra, String modelo, Long idUsuario) {
        executor.executar("Erro ao salvar o modelo de ciclos", () -> {
            jdbc.update(qGravarModelo, new MapSqlParameterSource("idObra", idObra).addValue("modelo", modelo).addValue("idUsuario", idUsuario));
        });
    }

    @Override
    public int gravarCelula(Long idObra, Long idTorre, String chave, String valor, Long idUsuario) {
        return executor.executar("Erro ao gravar a célula da escadinha", () -> jdbc.update(q.get("SPU.ESCADINHA.CELULA"),
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre).addValue("chave", chave)
                        .addValue("valor", valor, Types.VARCHAR).addValue("idUsuario", idUsuario)));
    }

    @Override
    public Optional<String> grade(Long idObra, Long idTorre) {
        return executor.executar("Erro ao ler a grade da escadinha", () -> jdbc.queryForList(q.get("SPS.ESCADINHA.GRADE"),
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre), String.class).stream().findFirst());
    }

    @Override
    public List<br.com.nord_tool_backend.dto.EscadinhaTipologiaDto> tipologias(Long idObra, Long idTorre, Long idTipologia) {
        return executor.executar("Erro ao listar as tipologias", () -> jdbc.query(q.get("SPS.ESCADINHA.TIPOLOGIAS"),
                new MapSqlParameterSource("idObra", idObra).addValue("idTorre", idTorre, Types.BIGINT).addValue("id", idTipologia, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(br.com.nord_tool_backend.dto.EscadinhaTipologiaDto.class)));
    }

    @Override
    public Optional<String> imagemTipologia(Long idObra, Long idTipologia) {
        return executor.executar("Erro ao ler a imagem da tipologia", () -> jdbc.queryForList(q.get("SPS.ESCADINHA.TIPOLOGIA.IMAGEM"),
                new MapSqlParameterSource("idObra", idObra).addValue("id", idTipologia), String.class).stream().findFirst());
    }

    @Override
    public boolean nomeTipologiaEmUso(Long idTorre, String nome, Long idIgnorar) {
        return executor.executar("Erro ao conferir a tipologia", () -> {
            Integer n = jdbc.queryForObject(q.get("SPS.ESCADINHA.TIPOLOGIA.NOME_EM_USO"), new MapSqlParameterSource("idTorre", idTorre)
                    .addValue("nome", nome).addValue("id", idIgnorar == null ? -1L : idIgnorar), Integer.class);
            return n != null && n > 0;
        });
    }

    @Override
    public Long inserirTipologia(Long idTorre, String nome, String imagem, Long idUsuario) {
        return executor.executar("Erro ao criar a tipologia", () -> jdbc.queryForObject(q.get("SPI.ESCADINHA.TIPOLOGIA"),
                new MapSqlParameterSource("idTorre", idTorre).addValue("nome", nome).addValue("imagem", imagem, Types.VARCHAR)
                        .addValue("idUsuario", idUsuario), Long.class));
    }

    @Override
    public int alterarTipologia(Long idObra, Long idTipologia, String nome, boolean mudarImagem, String imagem, Long idUsuario) {
        return executor.executar("Erro ao salvar a tipologia", () -> jdbc.update(q.get("SPU.ESCADINHA.TIPOLOGIA"),
                new MapSqlParameterSource("idObra", idObra).addValue("id", idTipologia).addValue("nome", nome)
                        .addValue("mudarImagem", mudarImagem).addValue("imagem", imagem, Types.VARCHAR).addValue("idUsuario", idUsuario)));
    }

    @Override
    public int excluirTipologia(Long idObra, Long idTipologia) {
        return executor.executar("Erro ao excluir a tipologia", () -> jdbc.update(q.get("SPD.ESCADINHA.TIPOLOGIA"),
                new MapSqlParameterSource("idObra", idObra).addValue("id", idTipologia)));
    }
}
