package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.form.AcessoUsuarioForm;

public interface AcessoService {
    AcessosDto listar();
    AcessoUsuarioDto salvar(Long idUsuario, AcessoUsuarioForm form);
}
