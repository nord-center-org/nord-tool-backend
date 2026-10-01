package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.RequisicaoChave;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NovaRetiradaControleChavesForm {
    @NotBlank
    private String nmTipoItem;

    @Positive
    private Long idApartamentoVistoria;

    @Positive
    private Long idFerramenta;

    @NotNull
    @Positive
    private Long idUserRetirada;

    @NotNull
    @Positive
    private Long idUserLiberacao;

    public RequisicaoChave converterToDto() {
        return RequisicaoChave.builder()
                .idApartamentoVistoria(idApartamentoVistoria)
                .idFerramenta(idFerramenta)
                .nmTipoItem(nmTipoItem)
                .idUserRetirada(idUserRetirada)
                .idUserLiberacao(idUserLiberacao)
                .build();
    }
}
