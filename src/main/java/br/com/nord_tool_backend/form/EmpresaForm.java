package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.Empresa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaForm {
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nmEmpresa;

    public Empresa converterToDomain(Long id) {
        return Empresa.builder().id(id).nmEmpresa(nmEmpresa).build();
    }
}
