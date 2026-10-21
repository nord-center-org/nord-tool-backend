package br.com.nord_tool_backend.form;

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
public class RecebimentoControleChavesForm {
    /** Ignorado: quem recebe é sempre o usuário logado. Mantido para clientes antigos. */
    private Long idUserRecebimento;
}
