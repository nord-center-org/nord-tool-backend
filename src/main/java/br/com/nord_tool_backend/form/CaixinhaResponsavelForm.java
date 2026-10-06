package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Size;

/** Criação (nome obrigatório) e atualização (nome e/ou ativo) de responsável. */
@Data
public class CaixinhaResponsavelForm {
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nmResponsavel;
    private Boolean inAtivo;
}
