package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CaixinhaResponsavel extends GlobalDomain {
    private String nmResponsavel;
    private Boolean inAtivo;
}
