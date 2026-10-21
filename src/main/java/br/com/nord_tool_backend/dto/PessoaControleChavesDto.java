package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Usuário que pode retirar chave ou ferramenta. */
@Data @AllArgsConstructor @NoArgsConstructor
public class PessoaControleChavesDto {
    private Long id;
    private String nmNome;
    private String nmCargo;
    private String nmEmpresa;
}
