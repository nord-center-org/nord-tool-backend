package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.GameConquistaDto;
import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GameRegistroDto;
import br.com.nord_tool_backend.dto.GameResumoDto;
import br.com.nord_tool_backend.form.GameCopaForm;
import br.com.nord_tool_backend.form.GamePartidaForm;

import java.util.List;

public interface GameService {

    GameRegistroDto registrarPartida(GamePartidaForm partida);

    GameResumoDto resumo(String jogo);

    List<GameConquistaDto> conquistas();

    List<String> favoritos();

    List<String> salvarFavoritos(List<String> jogos);

    GameCopaDto registrarCopa(GameCopaForm copa);

    List<GameCopaDto> copas();
}
