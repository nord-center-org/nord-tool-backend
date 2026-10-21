package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Obra;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class ObraDto {
    private Long id;
    private String nmObra;
    /** Código que o novo usuário digita no cadastro. */
    private String cdAcesso;
    private boolean inAtivo;
    private Integer nrVersao;
    private List<FaseDto> fases;

    public static ObraDto de(Obra o, List<FaseDto> fases) {
        return new ObraDto(o.getId(), o.getNmObra(), o.getCdAcesso(), !Boolean.FALSE.equals(o.getInAtivo()), o.getNrVersao(), fases);
    }
}
