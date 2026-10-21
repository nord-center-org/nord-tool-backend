package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class MedicaoStatus {
    private String cdStatus;
    private String nmStatus;
    private Integer nrOrdem;
    private Boolean inAtivo;
}
