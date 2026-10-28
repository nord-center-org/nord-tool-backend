package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.domain.IntegracaoToken;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;

import java.util.List;
import java.util.Optional;

/** Links de integração e as leituras feitas por eles (sem usuário logado: o dono do link é passado explicitamente). */
public interface IntegracaoRepository {
    List<IntegracaoToken> listar(Long idUsuario);

    int contarAtivos(Long idUsuario);

    Long criar(Long idUsuario, String escopo, String rotulo, String hash, String prefixo);

    int revogar(Long idUsuario, Long idToken);

    Optional<IntegracaoToken> buscarAtivoPorHash(String hash);

    void marcarUso(Long idToken);

    List<CronogramaSemanal> cronogramaDoUsuario(Long idUsuario, boolean verSemDono);

    List<CronogramaSemanalExcecaoDto> excecoesDoUsuario(Long idUsuario, boolean verSemDono);
}
