package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CronogramaEquipeDto;
import br.com.nord_tool_backend.form.CronogramaEquipeForm;

import java.util.List;

/**
 * Equipes do cronograma por obra: quem está na mesma equipe vê as demandas uns dos outros e pode movê-las na
 * esteira. Montar equipes é do admin do cronograma; a equipe pode nascer da hierarquia (líder + quem está abaixo).
 */
public interface CronogramaEquipeService {
    /** Equipes que eu enxergo, de todas as obras (para o filtro). */
    List<CronogramaEquipeDto> minhasEquipes();

    /** Equipes da obra escolhida (para montar). */
    List<CronogramaEquipeDto> equipesDaObra();

    CronogramaEquipeDto criar(CronogramaEquipeForm form);

    CronogramaEquipeDto alterar(Long id, CronogramaEquipeForm form);

    void excluir(Long id);

    /** Sugestão de membros: o líder e todos abaixo dele na hierarquia. */
    List<Long> pelaHierarquia(Long idLider);
}
