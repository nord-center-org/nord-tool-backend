package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;

import java.util.List;

public interface ContextoFaseRepository {
    boolean existeFase();
    /** Fases ativas de obras ativas; idObra nulo = todas. */
    List<Fase> fasesAtivas(Long idObra);
    /** Fases ativas (de obras ativas) em que o usuário está; idObra nulo = todas. */
    List<Fase> fasesDoUsuario(Long idUsuario, Long idObra);
    List<Obra> obrasAtivas();
}
