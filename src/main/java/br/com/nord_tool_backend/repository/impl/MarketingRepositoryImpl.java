package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.MarketingContaRegistroDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;
import br.com.nord_tool_backend.repository.MarketingRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class MarketingRepositoryImpl implements MarketingRepository {

    private static final String ID_USUARIO = "idUsuario";
    private static final String ID_CONTA = "idConta";
    private static final String ACESSO = "acesso";
    private static final String RENOVACAO = "renovacao";
    private static final String EXPIRA = "expira";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public MarketingRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String q : new String[]{"SPS.MARKETING.CONTAS", "SPS.MARKETING.CONTA", "SPI.MARKETING.CONTA", "SPU.MARKETING.TOKENS",
                "SPD.MARKETING.CONTA", "SPI.MARKETING.PUBLICACAO", "SPU.MARKETING.PUBLICACAO", "SPU.MARKETING.PRESAS",
                "SPS.MARKETING.PUBLICACOES"}) {
            queries.get(q);
        }
    }

    private static Timestamp ts(LocalDateTime d) {
        return d == null ? null : Timestamp.valueOf(d);
    }

    @Override
    public List<MarketingContaRegistroDto> contas(Long idUsuario) {
        return executor.executar("Erro ao ler as contas conectadas", () -> jdbc.query(queries.get("SPS.MARKETING.CONTAS"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario), BeanPropertyRowMapper.newInstance(MarketingContaRegistroDto.class)));
    }

    @Override
    public Optional<MarketingContaRegistroDto> conta(Long idUsuario, Long idConta) {
        List<MarketingContaRegistroDto> r = executor.executar("Erro ao ler a conta", () -> jdbc.query(queries.get("SPS.MARKETING.CONTA"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(ID_CONTA, idConta),
                BeanPropertyRowMapper.newInstance(MarketingContaRegistroDto.class)));
        return r.stream().findFirst();
    }

    @Override
    public Long salvarConta(Long idUsuario, String rede, String idExterno, String nome, String foto, String acesso, String renovacao,
                            LocalDateTime expira) {
        MapSqlParameterSource p = new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue("rede", rede).addValue("idExterno", idExterno)
                .addValue("nome", nome).addValue("foto", foto).addValue(ACESSO, acesso).addValue(RENOVACAO, renovacao)
                .addValue(EXPIRA, ts(expira));
        return executor.executar("Erro ao salvar a conta", () -> jdbc.queryForObject(queries.get("SPI.MARKETING.CONTA"), p, Long.class));
    }

    @Override
    public void atualizarTokens(Long idConta, String acesso, String renovacao, LocalDateTime expira) {
        MapSqlParameterSource p = new MapSqlParameterSource(ID_CONTA, idConta).addValue(ACESSO, acesso).addValue(RENOVACAO, renovacao)
                .addValue(EXPIRA, ts(expira));
        executor.executar("Erro ao renovar o acesso", () -> jdbc.update(queries.get("SPU.MARKETING.TOKENS"), p));
    }

    @Override
    public boolean excluirConta(Long idUsuario, Long idConta) {
        return executor.executar("Erro ao desconectar a conta", () -> jdbc.update(queries.get("SPD.MARKETING.CONTA"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(ID_CONTA, idConta))) > 0;
    }

    @Override
    public Long criarPublicacao(Long idUsuario, Long idConta, String titulo, String legenda, boolean publico) {
        MapSqlParameterSource p = new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(ID_CONTA, idConta).addValue("titulo", titulo)
                .addValue("legenda", legenda).addValue("publico", publico);
        return executor.executar("Erro ao registrar a publicação", () -> jdbc.queryForObject(queries.get("SPI.MARKETING.PUBLICACAO"), p, Long.class));
    }

    @Override
    public void concluirPublicacao(Long id, String status, String idExterno, String link, String aviso) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id).addValue("status", status).addValue("idExterno", idExterno)
                .addValue("link", link).addValue("aviso", aviso);
        executor.executar("Erro ao atualizar a publicação", () -> jdbc.update(queries.get("SPU.MARKETING.PUBLICACAO"), p));
    }

    @Override
    public void marcarPresas(Long idUsuario) {
        executor.executar("Erro ao atualizar as publicações", () -> jdbc.update(queries.get("SPU.MARKETING.PRESAS"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario)));
    }

    @Override
    public List<MarketingPublicacaoDto> publicacoes(Long idUsuario, int limite) {
        return executor.executar("Erro ao ler as publicações", () -> jdbc.query(queries.get("SPS.MARKETING.PUBLICACOES"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue("limite", limite),
                BeanPropertyRowMapper.newInstance(MarketingPublicacaoDto.class)));
    }
}
