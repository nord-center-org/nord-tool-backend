package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GamePartidaDto;
import br.com.nord_tool_backend.dto.GameRankingDto;
import br.com.nord_tool_backend.dto.GameTotaisDto;
import br.com.nord_tool_backend.form.GamePartidaForm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GameRepository {

    void registrarPartida(Long idUsuario, GamePartidaForm partida);

    /** Totais da pessoa num jogo; com {@code jogo} nulo, em todos os jogos. */
    GameTotaisDto totais(Long idUsuario, String jogo);

    List<GameRankingDto> ranking(String jogo, int limite);

    List<GamePartidaDto> historico(Long idUsuario, String jogo, int limite);

    /** Código da conquista → quando foi ganha. */
    Map<String, LocalDateTime> conquistas(Long idUsuario);

    /** Verdadeiro só quando a conquista é nova. */
    boolean ganharConquista(Long idUsuario, String conquista);

    Optional<String> favoritos(Long idUsuario);

    void salvarFavoritos(Long idUsuario, String jogos);

    Long registrarCopa(Long idUsuario, int rodadas, int colocacao, String resultado);

    List<GameCopaDto> copas(Long idUsuario, int limite);
}
