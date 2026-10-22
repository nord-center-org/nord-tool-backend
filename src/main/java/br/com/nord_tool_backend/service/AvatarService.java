package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.form.AvatarForm;

import java.util.List;

public interface AvatarService {

    AvatarDto meu();

    AvatarDto salvar(AvatarForm form);

    List<AvatarDto> equipe();
}
