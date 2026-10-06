package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CaixinhaLancamento extends GlobalDomain {
    private String cdRequisicao;
    private LocalDate dtLancamento;
    private Long idResponsavel;
    /** Vem do join com caixinha_responsavel. */
    private String nmResponsavel;
    private String txInsumo;
    private BigDecimal vlValor;
    private Boolean inLancado;
    private Boolean inPago;
    private Integer nrVersao;
    private LocalDateTime dhAlteracao;
    private Integer qtComprovantes;
}
