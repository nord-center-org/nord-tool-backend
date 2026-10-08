package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Uma categoria na conta do mês: o que já foi lançado e o que se espera até o fim do mês. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroProjecaoLinhaDto {
    private Long idCategoria;
    private String nmCategoria;
    private String cdTipo;
    private String cdProjecao;
    /** Soma dos lançamentos do mês. */
    private BigDecimal real;
    /** Valor que entra na conta: o real ou a estimativa. */
    private BigDecimal projetado;
    /** REAL | MEDIA | RECORRENCIA | RITMO | SEM_DADOS */
    private String origem;
    /** Explicação curta de como o valor foi obtido. */
    private String detalhe;
}
