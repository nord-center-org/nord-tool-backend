package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoDashboardDto {
    private CasamentoConfiguracaoDto configuracao;
    private Integer qtFornecedores;
    private Integer qtFornecedoresContratados;
    /** Soma dos valores dos fornecedores CONTRATADOS. */
    private BigDecimal vlContratado;
    private Integer qtConvidados;
    private Integer qtConvidadosConfirmados;
    /** Convidados confirmados + seus acompanhantes. */
    private Integer qtPessoasConfirmadas;
    private Integer qtMarcos;
    private Integer qtMarcosConcluidos;
    /** Marcos concluídos / total × 100 (inteiro arredondado; 0 sem marcos). */
    private Integer pcMarcosConcluidos;
    /** Até 5 marcos não concluídos, por prazo. */
    private List<CasamentoMarcoDto> proximosMarcos = new ArrayList<>();
}
