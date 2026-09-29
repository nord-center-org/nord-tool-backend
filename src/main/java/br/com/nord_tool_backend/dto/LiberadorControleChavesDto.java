package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiberadorControleChavesDto {
    private Long idUserLiberacao;
    private String nmPessoaLiberador;
    private String nmPermissaoLiberador;
}
