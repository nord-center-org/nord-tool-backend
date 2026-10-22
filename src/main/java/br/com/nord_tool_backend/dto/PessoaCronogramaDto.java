package br.com.nord_tool_backend.dto;

import lombok.Data;

/** Pessoa com quem dá para compartilhar um item do cronograma. */
@Data
public class PessoaCronogramaDto {
    private Long id;
    private String nmNome;
    private String nmCargo;
    /** Está abaixo do usuário logado na hierarquia (equipe). */
    private Boolean inEquipe;
}
