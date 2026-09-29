package br.com.nord_tool_backend.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NovaRetiradaControleChavesForm {
    private Long idApartamento;
    private Long idRetirante;
    private Long idLiberador;
}
