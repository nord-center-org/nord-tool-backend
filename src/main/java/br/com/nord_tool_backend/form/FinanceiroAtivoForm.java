package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Size;

/** Criação (ticker e pessoa obrigatórios) e atualização (nome, ativo e nrVersao). */
@Data
public class FinanceiroAtivoForm {
    @Size(max = 12, message = "O código do fundo deve ter no máximo 12 caracteres")
    private String cdTicker;
    private Long idPessoa;
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
    private String nmAtivo;
    private Boolean inAtivo;
    private Integer nrVersao;
}
