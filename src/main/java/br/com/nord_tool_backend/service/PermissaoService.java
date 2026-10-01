package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.form.PermissaoForm;
import java.util.List;

public interface PermissaoService {
    List<PermissaoDto> listarPermissoes();
    PermissaoDto salvarPermissao(PermissaoForm permissaoForm);
    PermissaoDto alterarPermissao(Long id, PermissaoForm permissaoForm);
    void deletarPermissao(Long id);
}
