package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class FinanceiroLancamento extends GlobalDomain {
    private String cdRequisicao;
    /** Dia 1 do mês a que o lançamento pertence. */
    private LocalDate dtCompetencia;
    private LocalDate dtLancamento;
    private Long idCategoria;
    /** Vêm dos joins com categoria, pessoa e usuário. */
    private String nmCategoria;
    private String cdTipo;
    private Long idPessoa;
    private String nmPessoa;
    private String dsLancamento;
    private BigDecimal vlLancamento;
    private Boolean inRealizado;
    /** Preenchido nos lançamentos gerados por uma recorrência. */
    private Long idRecorrencia;
    private Integer nrParcela;
    private Integer qtParcela;
    private Long idUsuarioCriacao;
    private String nmUsuarioCriacao;
    private Integer nrVersao;
    private LocalDateTime dhAlteracao;
}
