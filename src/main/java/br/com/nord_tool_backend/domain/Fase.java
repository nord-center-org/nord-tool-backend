package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Fase {
    private Long id;
    private Long idObra;
    private String nmFase;
    private String cdFase;
    private Integer nrOrdem;
    private Boolean inAtivo;
    private Integer nrVersao;
}
