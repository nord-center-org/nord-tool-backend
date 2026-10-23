package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.DaviJobRankingDto;

import java.util.List;
import java.util.Optional;

public interface DaviJobRepository {

    void inserir(Long idUsuario, int nrPontos);

    /** Melhor pontuação de cada usuário ativo, da maior para a menor (empate: quem fez primeiro). */
    List<DaviJobRankingDto> ranking(Long idUsuarioLogado, int limite);

    Optional<DaviJobRankingDto> recorde(Long idUsuario);
}
