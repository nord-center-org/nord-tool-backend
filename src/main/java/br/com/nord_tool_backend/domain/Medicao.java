package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class Medicao extends GlobalDomain {
    private Integer nrMedicao;
    private String cdContrato;
    private String nmRazaoSocial;
    private String nrContrato;
    private LocalDate dtMedicao;
    private BigDecimal vlMedicao;
    private String txMemorial;
    private String cdStatus;
    /** Vem do join com medicao_status. */
    private String nmStatus;
    private Integer nrVersao;
    private LocalDateTime dhAlteracao;
}
