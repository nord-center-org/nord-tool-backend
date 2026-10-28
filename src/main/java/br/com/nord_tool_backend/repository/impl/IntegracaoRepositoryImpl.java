package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.domain.IntegracaoToken;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.repository.IntegracaoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * O pool roda com auto-commit desligado: cada operação tem a própria transação. As leituras do cronograma
 * reaproveitam as queries da tela, com o dono do link no lugar do usuário logado.
 */
@Repository
@Transactional
public class IntegracaoRepositoryImpl implements IntegracaoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qContar;
    private final String qCriar;
    private final String qRevogar;
    private final String qPorHash;
    private final String qUso;
    private final String qCronograma;
    private final String qExcecoes;

    public IntegracaoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.INTEGRACAO.TOKENS");
        this.qContar = queries.get("SPS.INTEGRACAO.TOKENS.CONTAR");
        this.qCriar = queries.get("SPI.INTEGRACAO.TOKEN");
        this.qRevogar = queries.get("SPU.INTEGRACAO.TOKEN.REVOGAR");
        this.qPorHash = queries.get("SPS.INTEGRACAO.TOKEN.HASH");
        this.qUso = queries.get("SPU.INTEGRACAO.TOKEN.USO");
        this.qCronograma = queries.get("SPS.CRONOGRAMA_SEMANAL.LISTAR");
        this.qExcecoes = queries.get("SPS.CRONOGRAMA_SEMANAL_EXCECAO.LISTAR");
    }

    @Override
    public List<IntegracaoToken> listar(Long idUsuario) {
        return executor.executar("Erro ao listar os links de integração", () -> jdbc.query(qListar,
                new MapSqlParameterSource("idUsuario", idUsuario), BeanPropertyRowMapper.newInstance(IntegracaoToken.class)));
    }

    @Override
    public int contarAtivos(Long idUsuario) {
        return executor.executar("Erro ao contar os links de integração", () -> {
            Integer n = jdbc.queryForObject(qContar, new MapSqlParameterSource("idUsuario", idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public Long criar(Long idUsuario, String escopo, String rotulo, String hash, String prefixo) {
        return executor.executar("Erro ao criar o link de integração", () -> jdbc.queryForObject(qCriar,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("escopo", escopo).addValue("rotulo", rotulo)
                        .addValue("hash", hash).addValue("prefixo", prefixo), Long.class));
    }

    @Override
    public int revogar(Long idUsuario, Long idToken) {
        return executor.executar("Erro ao revogar o link de integração", () -> jdbc.update(qRevogar,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", idToken)));
    }

    @Override
    public Optional<IntegracaoToken> buscarAtivoPorHash(String hash) {
        return executor.executar("Erro ao conferir o link de integração", () -> jdbc.query(qPorHash,
                new MapSqlParameterSource("hash", hash), BeanPropertyRowMapper.newInstance(IntegracaoToken.class)).stream().findFirst());
    }

    @Override
    public void marcarUso(Long idToken) {
        executor.executar("Erro ao registrar o uso do link", () -> {
            jdbc.update(qUso, new MapSqlParameterSource("id", idToken));
        });
    }

    @Override
    public List<CronogramaSemanal> cronogramaDoUsuario(Long idUsuario, boolean verSemDono) {
        return executor.executar("Erro ao ler o cronograma", () -> jdbc.query(qCronograma,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("verSemDono", verSemDono).addValue("verTudo", false).addValue("verEquipe", false),
                BeanPropertyRowMapper.newInstance(CronogramaSemanal.class)));
    }

    @Override
    public List<CronogramaSemanalExcecaoDto> excecoesDoUsuario(Long idUsuario, boolean verSemDono) {
        return executor.executar("Erro ao ler as exceções do cronograma", () -> jdbc.query(qExcecoes,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("verSemDono", verSemDono).addValue("verTudo", false).addValue("verEquipe", false),
                BeanPropertyRowMapper.newInstance(CronogramaSemanalExcecaoDto.class)));
    }
}
