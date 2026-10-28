package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.CronogramaEquipe;

import java.util.Collection;
import java.util.List;

/** Equipes do cronograma por obra e seus membros. */
public interface CronogramaEquipeRepository {
    /** Equipes que a pessoa enxerga (membro, ou todas se verTudo); idObra nulo = todas as obras. */
    List<CronogramaEquipe> listar(Long idObra, Long idUsuario, boolean verTudo);

    Long criar(Long idObra, String nmEquipe, Long idLider);

    int alterar(Long id, Long idObra, String nmEquipe, Long idLider);

    int excluir(Long id, Long idObra);

    boolean nomeEmUso(Long idObra, String nmEquipe, Long idIgnorar);

    void substituirMembros(Long id, Collection<Long> idsUsuarios);

    /** O líder e todos abaixo dele na hierarquia (ativos e aprovados). */
    List<Long> hierarquia(Long idLider);
}
