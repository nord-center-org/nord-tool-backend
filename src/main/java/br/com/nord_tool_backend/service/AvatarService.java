package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.form.AvatarForm;

import java.util.List;
import java.util.Optional;

public interface AvatarService {

    AvatarDto meu();

    AvatarDto salvar(AvatarForm form);

    List<AvatarDto> equipe();

    /** Quem anuncia as novidades na tela inicial (só o primeiro nome); vazio quando não há ninguém para isso. */
    Optional<AvatarDto> portaVoz();
}
