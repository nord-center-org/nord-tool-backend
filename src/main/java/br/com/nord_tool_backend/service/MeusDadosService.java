package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.MeusDadosDto;
import br.com.nord_tool_backend.form.MeusDadosForm;

public interface MeusDadosService {

    MeusDadosDto ler();

    MeusDadosDto salvar(MeusDadosForm form);
}
