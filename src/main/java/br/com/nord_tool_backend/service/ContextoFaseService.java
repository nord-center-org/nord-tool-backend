package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.security.EscopoFase;

import java.util.List;

/**
 * Obra e fase da requisição. O frontend manda a obra escolhida no cabeçalho {@code X-Obra} (e, ao criar,
 * a fase em {@code X-Fase}); o backend só aceita as fases em que o usuário está (o admin vê todas).
 */
public interface ContextoFaseService {
    String CABECALHO_OBRA = "X-Obra";
    String CABECALHO_FASE = "X-Fase";

    EscopoFase escopo();

    /** Fase em que um registro novo entra (cabeçalho X-Fase, sigla do código ou a única fase possível). */
    Long faseParaCriar(String codigoComSigla);

    /** Obras e fases que o usuário logado pode escolher. */
    List<ObraDto> minhasObras();
}
