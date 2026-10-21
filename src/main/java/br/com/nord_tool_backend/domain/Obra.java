package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Obra {
    private Long id;
    private String nmObra;
    private String cdAcesso;
    private Boolean inAtivo;
    private Integer nrVersao;
}
