package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetiranteControleChavesDto {
    private Long idUserRetirada;
    private String nmPessoaRetirante;
    private String nmPermissaoRetirante;
}
