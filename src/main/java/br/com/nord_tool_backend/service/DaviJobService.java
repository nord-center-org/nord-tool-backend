package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.DaviJobPlacarDto;
import br.com.nord_tool_backend.form.DaviJobPontuacaoForm;

public interface DaviJobService {

    DaviJobPlacarDto placar();

    /** Registra a partida e devolve o placar atualizado. */
    DaviJobPlacarDto registrar(DaviJobPontuacaoForm form);
}
