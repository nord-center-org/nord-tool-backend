package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Perfil;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.AcessoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AcessoRepositoryImpl implements AcessoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qUsuarios;
    private final String qPerfis;
    private final String qPermissoesPerfis;
    private final String qExcecoes;
    private final String qAlterar;
    private final String qApagarExcecoes;
    private final String qInserirExcecao;

    public AcessoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qUsuarios = queries.get("SPS.ACESSO.USUARIOS");
        this.qPerfis = queries.get("SPS.ACESSO.PERFIS");
        this.qPermissoesPerfis = queries.get("SPS.ACESSO.PERMISSOES_PERFIS");
        this.qExcecoes = queries.get("SPS.ACESSO.EXCECOES");
        this.qAlterar = queries.get("SPU.ACESSO.PERFIL_SITUACAO");
        this.qApagarExcecoes = queries.get("SPD.ACESSO.EXCECOES");
        this.qInserirExcecao = queries.get("SPI.ACESSO.EXCECAO");
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return executor.executar("Listar usuários", () ->
                jdbc.query(qUsuarios, BeanPropertyRowMapper.newInstance(Usuario.class)));
    }

    @Override
    public List<Perfil> listarPerfis() {
        return executor.executar("Listar perfis", () ->
                jdbc.query(qPerfis, BeanPropertyRowMapper.newInstance(Perfil.class)));
    }

    @Override
    public Map<Long, List<PerfilPermissao>> permissoesPorPerfil() {
        return agrupar("Listar permissões dos perfis", qPermissoesPerfis);
    }

    @Override
    public Map<Long, List<PerfilPermissao>> excecoesPorUsuario() {
        return agrupar("Listar exceções de acesso", qExcecoes);
    }

    @Override
    public void alterarPerfilESituacao(Long idUsuario, Long idPerfil, boolean ativo) {
        executor.executar("Alterar perfil do usuário", () -> jdbc.update(qAlterar, new MapSqlParameterSource()
                .addValue("id", idUsuario).addValue("idPerfil", idPerfil).addValue("ativo", ativo)));
    }

    @Override
    public void substituirExcecoes(Long idUsuario, List<PerfilPermissao> excecoes) {
        executor.executar("Salvar acessos do usuário", () -> {
            jdbc.update(qApagarExcecoes, new MapSqlParameterSource("id", idUsuario));
            for (PerfilPermissao e : excecoes) {
                jdbc.update(qInserirExcecao, new MapSqlParameterSource().addValue("id", idUsuario)
                        .addValue("cdModulo", e.getCdModulo()).addValue("cdAcao", e.getCdAcao()));
            }
        });
    }

    private Map<Long, List<PerfilPermissao>> agrupar(String operacao, String sql) {
        return executor.executar(operacao, () -> {
            Map<Long, List<PerfilPermissao>> mapa = new HashMap<>();
            jdbc.query(sql, rs -> {
                mapa.computeIfAbsent(rs.getLong("id"), k -> new ArrayList<>())
                        .add(new PerfilPermissao(rs.getString("cdModulo"), rs.getString("cdAcao")));
            });
            return mapa;
        });
    }
}
