package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Size;

/** Criação (nome, tipo e projeção obrigatórios) e atualização (qualquer campo informado) de categoria. */
@Data
public class FinanceiroCategoriaForm {
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nmCategoria;
    /** ENTRADA | SAIDA; não muda enquanto a categoria tiver lançamentos. */
    private String cdTipo;
    private String cdProjecao;
    private Boolean inFixa;
    /** DIA_MES | DIA_UTIL; exige nrDia. */
    private String cdRegraData;
    private Integer nrDia;
    private Integer nrOrdem;
    private Boolean inAtivo;
}
