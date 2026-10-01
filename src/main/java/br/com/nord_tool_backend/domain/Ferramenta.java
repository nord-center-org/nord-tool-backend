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
    private String nmCategoria;
    private String cdPatrimonio;
    private Boolean flAtivo;
}
