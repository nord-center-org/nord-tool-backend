package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @AllArgsConstructor @NoArgsConstructor
public class CaixinhaResumoDto {
    private BigDecimal total;
    private BigDecimal pago;
    /** total - pago. O Lombok gera getAPagar() e o Jackson viraria "apagar": o nome do JSON é fixado aqui. */
    @JsonProperty("aPagar")
    private BigDecimal aPagar;
    private Integer qtLancamentos;
    private Integer qtPagos;
    private Integer qtPendentes;
}
