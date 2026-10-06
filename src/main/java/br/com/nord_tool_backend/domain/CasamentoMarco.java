package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class CasamentoMarco extends GlobalDomain {
    private String nmTitulo;
    private LocalDate dtPrazo;
    private Boolean inConcluido;
    private String txObservacao;
}
