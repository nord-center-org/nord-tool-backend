package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Um fundo imobiliário de uma pessoa. A cotação guardada é a última conhecida. */
@Getter @Setter @NoArgsConstructor
public class FinanceiroAtivo extends GlobalDomain {
    private String cdTicker;
    private String nmAtivo;
    private String cdTipo;
    private Long idPessoa;
    /** Vem do join com a pessoa. */
    private String nmPessoa;
    private BigDecimal vlCotacao;
    private LocalDateTime dhCotacao;
    private Boolean inAtivo;
    private Integer nrVersao;
}
