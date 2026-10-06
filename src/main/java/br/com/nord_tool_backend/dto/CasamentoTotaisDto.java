package br.com.nord_tool_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Totais crus do dashboard (linha única da consulta agregada). */
@Data @NoArgsConstructor
public class CasamentoTotaisDto {
    private Integer qtFornecedores;
    private Integer qtFornecedoresContratados;
    private BigDecimal vlContratado;
    private Integer qtConvidados;
    private Integer qtConvidadosConfirmados;
    private Integer qtPessoasConfirmadas;
    private Integer qtMarcos;
    private Integer qtMarcosConcluidos;
}
