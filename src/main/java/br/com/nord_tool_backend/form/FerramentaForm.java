package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.Ferramenta;
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
public class FerramentaForm {
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    private String nmFerramenta;

    @Size(max = 60, message = "Categoria deve ter no máximo 60 caracteres")
    private String nmCategoria;

    @Size(max = 60, message = "Patrimônio deve ter no máximo 60 caracteres")
    private String cdPatrimonio;

    private Boolean flAtivo;

    public Ferramenta converterToDomain(Long id) {
        return Ferramenta.builder()
                .id(id)
                .nmFerramenta(nmFerramenta)
                .nmCategoria(nmCategoria)
                .cdPatrimonio(cdPatrimonio)
                .flAtivo(flAtivo != null ? flAtivo : Boolean.TRUE)
                .build();
    }
}
