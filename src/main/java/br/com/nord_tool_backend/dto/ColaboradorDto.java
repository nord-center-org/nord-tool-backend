package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Colaborador;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColaboradorDto {
    private Long id;
    private String nmColaborador;
    private String nrCelular;
    private Integer idEmpresa;
    private String nmEmpresa;
    private Integer idCargo;
    private String nmCargo;
    private Integer idPermissao;
    private String nmPermissao;

    public static ColaboradorDto converterToDto(Colaborador colaborador) {
        return ColaboradorDto.builder()
                .id(colaborador.getId())
                .nmColaborador(colaborador.getNmColaborador())
                .nrCelular(colaborador.getNrCelular())
                .idEmpresa(colaborador.getIdEmpresa())
                .nmEmpresa(colaborador.getNmEmpresa())
                .idCargo(colaborador.getIdCargo())
                .nmCargo(colaborador.getNmCargo())
                .idPermissao(colaborador.getIdPermissao())
                .nmPermissao(colaborador.getNmPermissao())
                .build();
    }
}
