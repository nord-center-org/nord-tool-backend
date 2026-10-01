package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ferramenta extends GlobalDomain {
    private Long id;
    private String nmFerramenta;
    private String nmFerramentaCategoria;
    private String cdFerramentaPatrimonio;
    private Boolean inFerramentaAtivo;
}
