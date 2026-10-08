package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo de um lançamento fixo (apartamento, evolução de obra, investimentos...). */
@Getter @Setter @NoArgsConstructor
public class FinanceiroRecorrencia extends GlobalDomain {
    private Long idCategoria;
    /** Vêm dos joins com categoria e pessoa. */
    private String nmCategoria;
    private String cdTipo;
    private Long idPessoa;
    private String nmPessoa;
    private String dsRecorrencia;
    private BigDecimal vlRecorrencia;
    /** Dia do mês do lançamento gerado (vazio = dia 1). */
    private Integer nrDia;
    private LocalDate dtInicio;
    private LocalDate dtFim;
    private Boolean inAtivo;
    private Integer nrVersao;
}
