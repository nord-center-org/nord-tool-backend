package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoDashboardDto;
import br.com.nord_tool_backend.form.CasamentoConfiguracaoForm;

public interface CasamentoService {

    /** Casal e data do casamento (nulos enquanto não configurados). */
    CasamentoConfiguracaoDto buscarConfiguracao();

    CasamentoConfiguracaoDto salvarConfiguracao(CasamentoConfiguracaoForm form);

    CasamentoDashboardDto dashboard();
}
