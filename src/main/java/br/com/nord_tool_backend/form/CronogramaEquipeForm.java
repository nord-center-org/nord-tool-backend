package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Equipe do cronograma na obra escolhida. O líder entra como membro automaticamente. */
@Data
public class CronogramaEquipeForm {
    @NotBlank
    @Size(max = 80)
    private String nmEquipe;
    private Long idLider;
    private List<Long> idsMembros = new ArrayList<>();
}
