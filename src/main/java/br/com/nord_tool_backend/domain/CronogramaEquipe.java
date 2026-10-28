package br.com.nord_tool_backend.domain;

import lombok.Data;

/** Linha de cronograma_equipe com os membros em texto ("3,7,9"). */
@Data
public class CronogramaEquipe {
    private Long id;
    private Long idObra;
    private String nmObra;
    private String nmEquipe;
    private Long idLider;
    private String nmLider;
    private String txMembros;
}
