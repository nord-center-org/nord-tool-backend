package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CasamentoConvidado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoConvidadoDto {
    private Long idConvidado;
    private String nmConvidado;
    private String nmGrupo;
    private String nrTelefone;
    private String nmRelacao;
    /** NAO_CONVIDADO | CONVIDADO | CONFIRMADO | NAO_IRA */
    private String nmStatus;
    private Integer nrAcompanhantes;
    private String nmMesa;

    public static CasamentoConvidadoDto de(CasamentoConvidado c) {
        return new CasamentoConvidadoDto(c.getId(), c.getNmConvidado(), c.getNmGrupo(), c.getNrTelefone(),
                c.getNmRelacao(), c.getNmStatus(), c.getNrAcompanhantes() == null ? 0 : c.getNrAcompanhantes(), c.getNmMesa());
    }
}
