package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Size;

/** Criação (nome obrigatório) e atualização (qualquer campo informado) de pessoa. */
@Data
public class FinanceiroPessoaForm {
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nmPessoa;
    private Boolean inCompartilhado;
    /** Login a que a pessoa corresponde (um login por pessoa). */
    private Long idUsuario;
    private Integer nrOrdem;
    private Boolean inAtivo;
}
