package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ObraRepository {
    List<Obra> listarObras();
    Optional<Obra> buscarObra(Long id);
    Long inserirObra(Obra obra);
    /** Devolve o nº de linhas (0 = versão divergente). */
    int alterarObra(Obra obra, int nrVersao);
    List<Fase> listarFases();
    Optional<Fase> buscarFase(Long id);
    Long inserirFase(Fase fase);
    int alterarFase(Fase fase, int nrVersao);
    /** Participantes por fase. */
    Map<Long, List<Long>> usuariosPorFase();
    void substituirUsuariosDaFase(Long idFase, List<Long> idsUsuarios);
    /** Quantos destes ids existem em usuario. */
    int contarUsuarios(List<Long> ids);
    /** Liga apartamentos sem fase pela sigla; devolve quantos foram ligados. */
    int vincularApartamentosPorSigla();
    int contarApartamentosSemFase();
}
