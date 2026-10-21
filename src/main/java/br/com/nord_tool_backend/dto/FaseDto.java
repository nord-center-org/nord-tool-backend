package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Fase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class FaseDto {
    private Long id;
    private Long idObra;
    private String nmFase;
    private String cdFase;
    private Integer nrOrdem;
    private boolean inAtivo;
    private Integer nrVersao;
    /** Usuários que participam da fase. */
    private List<Long> idsUsuarios;

    public static FaseDto de(Fase f, List<Long> idsUsuarios) {
        return new FaseDto(f.getId(), f.getIdObra(), f.getNmFase(), f.getCdFase(), f.getNrOrdem(),
                !Boolean.FALSE.equals(f.getInAtivo()), f.getNrVersao(), idsUsuarios);
    }
}
