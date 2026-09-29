package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.RequisicaoChave;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NovaRetiradaControleChavesForm {
    @NotNull
    @Positive
    private Long idApartamentoVistoria;

    @NotNull
    @Positive
    private Long idUserRetirada;

    @NotNull
    @Positive
    private Long idUserLiberacao;

    public RequisicaoChave converterToDto() {
        return RequisicaoChave.builder()
                .idApartamentoVistoria(idApartamentoVistoria)
                .idUserRetirada(idUserRetirada)
                .idUserLiberacao(idUserLiberacao)
                .build();
    }
}
