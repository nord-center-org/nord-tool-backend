package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CaixinhaResponsavel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class CaixinhaResponsavelDto {
    private Long idResponsavel;
    private String nmResponsavel;
    private boolean inAtivo;

    public static CaixinhaResponsavelDto de(CaixinhaResponsavel r) {
        return new CaixinhaResponsavelDto(r.getId(), r.getNmResponsavel(), !Boolean.FALSE.equals(r.getInAtivo()));
    }
}
