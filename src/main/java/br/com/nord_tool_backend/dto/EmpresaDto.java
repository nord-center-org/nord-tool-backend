package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Empresa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaDto {
    private Long id;
    private String nmEmpresa;

    public static EmpresaDto converterToDto(Empresa empresa) {
        return EmpresaDto.builder()
                .id(empresa.getId())
                .nmEmpresa(empresa.getNmEmpresa())
                .build();
    }
}
