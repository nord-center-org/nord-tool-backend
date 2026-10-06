package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @AllArgsConstructor @NoArgsConstructor
public class CaixinhaResumoDto {
    private BigDecimal total;
    private BigDecimal pago;
    /** total - pago */
    private BigDecimal aPagar;
    private Integer qtLancamentos;
    private Integer qtPagos;
    private Integer qtPendentes;
}
