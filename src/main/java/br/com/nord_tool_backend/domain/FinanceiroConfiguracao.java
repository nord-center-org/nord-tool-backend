package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class FinanceiroConfiguracao extends GlobalDomain {
    private BigDecimal vlMetaSaldo;
    private Integer nrMesesMedia;
    private Integer nrDiaConferencia;
    private Integer nrDiaFechamentoFatura;
}
