package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.AvatarDto;

import java.util.List;
import java.util.Optional;

public interface AvatarRepository {

    Optional<AvatarDto> buscar(Long idUsuario);

    void salvar(Long idUsuario, String dsConfig);

    /** O usuário, o superior, os colegas do mesmo superior e toda a equipe abaixo dele (ativos e aprovados). */
    List<AvatarDto> equipe(Long idUsuario, int limite);
}
