package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** De quem é um lançamento (Nick, Thaina, Casal). Diferente do autor, que é o usuário logado. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroPessoa extends GlobalDomain {
    private String nmPessoa;
    private Boolean inCompartilhado;
    private Long idUsuario;
    private Integer nrOrdem;
    private Boolean inAtivo;
}
