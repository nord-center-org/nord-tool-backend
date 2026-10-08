package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroCategoriaDto {
    private Long idCategoria;
    private String nmCategoria;
    private String cdTipo;
    private String cdProjecao;
    private boolean inFixa;
    private String cdRegraData;
    private Integer nrDia;
    private Integer nrOrdem;
    private boolean inAtivo;

    public static FinanceiroCategoriaDto de(FinanceiroCategoria c) {
        return new FinanceiroCategoriaDto(c.getId(), c.getNmCategoria(), c.getCdTipo(), c.getCdProjecao(),
                Boolean.TRUE.equals(c.getInFixa()), c.getCdRegraData(), c.getNrDia(), c.getNrOrdem(),
                !Boolean.FALSE.equals(c.getInAtivo()));
    }
}
