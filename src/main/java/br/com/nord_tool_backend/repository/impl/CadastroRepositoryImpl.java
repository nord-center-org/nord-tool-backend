package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.repository.CadastroRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/** Nunca registra parâmetros (hash/token): o JdbcExecutor só loga a operação e o SQLState. */
@Repository
public class CadastroRepositoryImpl implements CadastroRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries q;

    public CadastroRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.q = queries;
        for (String chave : new String[]{"SPS.CADASTRO.OBRA_POR_CODIGO", "SPS.CADASTRO.EMAIL_EXISTE", "SPI.CADASTRO.USUARIO",
                "SPI.CADASTRO.TOKEN", "SPS.CADASTRO.TOKEN", "SPU.CADASTRO.USAR_TOKEN", "SPU.CADASTRO.INVALIDAR_TOKENS"}) {
            queries.get(chave);
        }
    }

    @Override
    public Optional<Obra> buscarObraAtivaPorCodigo(String cdAcesso) {
        return executor.executar("Buscar obra pelo código", () -> jdbc.query(q.get("SPS.CADASTRO.OBRA_POR_CODIGO"),
                new MapSqlParameterSource("cd", cdAcesso), BeanPropertyRowMapper.newInstance(Obra.class)).stream().findFirst());
    }

    @Override
    public boolean emailExiste(String email) {
        return executor.executar("Conferir e-mail", () -> {
            Long n = jdbc.queryForObject(q.get("SPS.CADASTRO.EMAIL_EXISTE"), new MapSqlParameterSource("email", email), Long.class);
            return n != null && n > 0;
        });
    }

    @Override
    public Long inserirPendente(String nome, String email, String hash, String celular, Long idObra) {
        return executor.executar("Salvar cadastro", () -> jdbc.queryForObject(q.get("SPI.CADASTRO.USUARIO"), new MapSqlParameterSource()
                .addValue("nome", nome).addValue("email", email).addValue("hash", hash)
                .addValue("celular", celular).addValue("idObra", idObra, java.sql.Types.BIGINT), Long.class));
    }

    @Override
    public void inserirToken(Long idUsuario, String hashToken, LocalDateTime expira) {
        executor.executar("Salvar pedido de nova senha", () -> jdbc.update(q.get("SPI.CADASTRO.TOKEN"), new MapSqlParameterSource()
                .addValue("idUsuario", idUsuario).addValue("hash", hashToken).addValue("expira", Timestamp.valueOf(expira))));
    }

    @Override
    public Optional<TokenRedefinicao> buscarToken(String hashToken) {
        return executor.executar("Buscar pedido de nova senha", () -> jdbc.query(q.get("SPS.CADASTRO.TOKEN"),
                new MapSqlParameterSource("hash", hashToken), (rs, i) -> new TokenRedefinicao(rs.getLong("id"), rs.getLong("idUsuario"),
                        rs.getTimestamp("expira").toLocalDateTime(),
                        rs.getTimestamp("uso") == null ? null : rs.getTimestamp("uso").toLocalDateTime())).stream().findFirst());
    }

    @Override
    public boolean usarToken(Long idToken) {
        return executor.executar("Usar pedido de nova senha", () ->
                jdbc.update(q.get("SPU.CADASTRO.USAR_TOKEN"), new MapSqlParameterSource("id", idToken)) == 1);
    }

    @Override
    public void invalidarTokens(Long idUsuario) {
        executor.executar("Invalidar pedidos de nova senha", () ->
                jdbc.update(q.get("SPU.CADASTRO.INVALIDAR_TOKENS"), new MapSqlParameterSource("idUsuario", idUsuario)));
    }
}
