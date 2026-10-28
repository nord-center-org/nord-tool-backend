package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Equipe do cronograma numa obra: os membros veem as demandas uns dos outros. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronogramaEquipeDto {
    private Long id;
    private Long idObra;
    private String nmObra;
    private String nmEquipe;
    private Long idLider;
    private String nmLider;
    private List<Long> idsMembros;
}
