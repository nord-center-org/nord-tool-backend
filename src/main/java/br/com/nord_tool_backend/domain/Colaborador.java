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
public class Colaborador extends GlobalDomain {
    private Long id;
    private String nmColaborador;
    private String nrCelular;
    private Integer idEmpresa;
    private String nmEmpresa;
    private Integer idCargo;
    private String nmCargo;
    private Integer idPermissao;
    private String nmPermissao;
}
