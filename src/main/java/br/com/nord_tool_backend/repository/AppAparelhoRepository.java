package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.AppAparelhoDto;

import java.util.Optional;

/** Aparelhos com o app Android (só o hash do token fica no banco). */
public interface AppAparelhoRepository {
    void criar(Long idUsuario, String hash, String aparelho, int versaoSessao);

    /** Aparelho não revogado e usado nos últimos {@code dias} dias. */
    Optional<AppAparelhoDto> porHash(String hash, int dias);

    void marcarUso(Long id);

    int revogar(String hash);

    /** Revoga os aparelhos da pessoa além dos {@code limite} usados mais recentemente. */
    int limitar(Long idUsuario, int limite);
}
