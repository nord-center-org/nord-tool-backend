package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.MedicaoStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class MedicaoStatusDto {
    private String cdStatus;
    private String nmStatus;
    private boolean inAtivo;

    public static MedicaoStatusDto de(MedicaoStatus s) {
        return new MedicaoStatusDto(s.getCdStatus(), s.getNmStatus(), !Boolean.FALSE.equals(s.getInAtivo()));
    }
}
