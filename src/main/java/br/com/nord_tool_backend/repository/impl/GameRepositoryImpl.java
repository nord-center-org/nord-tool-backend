package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GamePartidaDto;
import br.com.nord_tool_backend.dto.GameRankingDto;
import br.com.nord_tool_backend.dto.GameTotaisDto;
import br.com.nord_tool_backend.form.GamePartidaForm;
import br.com.nord_tool_backend.repository.GameRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class GameRepositoryImpl implements GameRepository {

    private static final String ID_USUARIO = "idUsuario";
    private static final String JOGO = "jogo";
    private static final String LIMITE = "limite";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public GameRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String q : new String[]{"SPI.GAME.PARTIDA", "SPS.GAME.TOTAIS", "SPS.GAME.RANKING", "SPS.GAME.HISTORICO", "SPS.GAME.CONQUISTAS",
                "SPI.GAME.CONQUISTA", "SPS.GAME.FAVORITOS", "SPI.GAME.FAVORITOS", "SPI.GAME.COPA", "SPS.GAME.COPAS"}) {
            queries.get(q);
        }
    }

    @Override
    public void registrarPartida(Long idUsuario, GamePartidaForm p) {
        MapSqlParameterSource params = new MapSqlParameterSource(ID_USUARIO, idUsuario)
                .addValue(JOGO, p.getCdJogo()).addValue("modo", p.getCdModo()).addValue("pontos", p.getNrPontos())
                .addValue("vitoria", p.isInVitoria()).addValue("colocacao", p.getNrColocacao()).addValue("jogadores", p.getNrJogadores())
                .addValue("bots", p.getNrBots()).addValue("dificuldade", p.getCdDificuldade()).addValue("duracao", p.getNrDuracaoS())
                .addValue("copa", p.getIdCopa());
        executor.executar("Erro ao registrar a partida", () -> jdbc.update(queries.get("SPI.GAME.PARTIDA"), params));
    }

    @Override
    public GameTotaisDto totais(Long idUsuario, String jogo) {
        return executor.executar("Erro ao ler os recordes", () -> jdbc.queryForObject(queries.get("SPS.GAME.TOTAIS"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(JOGO, jogo), BeanPropertyRowMapper.newInstance(GameTotaisDto.class)));
    }

    @Override
    public List<GameRankingDto> ranking(String jogo, int limite) {
        return executor.executar("Erro ao ler o ranking", () -> jdbc.query(queries.get("SPS.GAME.RANKING"),
                new MapSqlParameterSource(JOGO, jogo).addValue(LIMITE, limite), BeanPropertyRowMapper.newInstance(GameRankingDto.class)));
    }

    @Override
    public List<GamePartidaDto> historico(Long idUsuario, String jogo, int limite) {
        return executor.executar("Erro ao ler o histórico", () -> jdbc.query(queries.get("SPS.GAME.HISTORICO"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(JOGO, jogo).addValue(LIMITE, limite),
                BeanPropertyRowMapper.newInstance(GamePartidaDto.class)));
    }

    @Override
    public Map<String, LocalDateTime> conquistas(Long idUsuario) {
        return executor.executar("Erro ao ler as conquistas", () -> {
            Map<String, LocalDateTime> mapa = new LinkedHashMap<>();
            jdbc.query(queries.get("SPS.GAME.CONQUISTAS"), new MapSqlParameterSource(ID_USUARIO, idUsuario), rs -> {
                Timestamp quando = rs.getTimestamp("dhCriacao");
                mapa.put(rs.getString("cdConquista"), quando == null ? null : quando.toLocalDateTime());
            });
            return mapa;
        });
    }

    @Override
    public boolean ganharConquista(Long idUsuario, String conquista) {
        return executor.executar("Erro ao salvar a conquista", () -> jdbc.update(queries.get("SPI.GAME.CONQUISTA"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue("conquista", conquista)) > 0);
    }

    @Override
    public Optional<String> favoritos(Long idUsuario) {
        return executor.executar("Erro ao ler os favoritos dos games", () ->
                jdbc.queryForList(queries.get("SPS.GAME.FAVORITOS"), new MapSqlParameterSource(ID_USUARIO, idUsuario), String.class)
                        .stream().findFirst());
    }

    @Override
    public void salvarFavoritos(Long idUsuario, String jogos) {
        executor.executar("Erro ao salvar os favoritos dos games", () ->
                jdbc.update(queries.get("SPI.GAME.FAVORITOS"), new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue("jogos", jogos)));
    }

    @Override
    public Long registrarCopa(Long idUsuario, int rodadas, int colocacao, String resultado) {
        return executor.executar("Erro ao registrar a Copa", () -> jdbc.queryForObject(queries.get("SPI.GAME.COPA"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue("rodadas", rodadas).addValue("colocacao", colocacao)
                        .addValue("resultado", resultado), Long.class));
    }

    @Override
    public List<GameCopaDto> copas(Long idUsuario, int limite) {
        return executor.executar("Erro ao ler as Copas", () -> jdbc.query(queries.get("SPS.GAME.COPAS"),
                new MapSqlParameterSource(ID_USUARIO, idUsuario).addValue(LIMITE, limite), BeanPropertyRowMapper.newInstance(GameCopaDto.class)));
    }
}
