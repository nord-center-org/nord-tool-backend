package br.com.nord_tool_backend.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecebimentoControleChavesForm {
    /** Ignorado: quem recebe é sempre o usuário logado. Mantido para clientes antigos. */
    private Long idUserRecebimento;
}
