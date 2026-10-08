package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** A aba Investimentos: os fundos e os totais. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroInvestimentoDto {
    private List<FinanceiroAtivoDto> ativos;
    private BigDecimal vlInvestido;
    private BigDecimal vlPatrimonio;
    private BigDecimal vlResultado;
    private BigDecimal pcResultado;
    private BigDecimal vlAReceber;
    /** Parte do a receber que cai no mês corrente. */
    private BigDecimal vlAReceberMes;
    /** Falso quando nenhuma cotação ao vivo veio (provedor fora do ar ou sem token). */
    private boolean cotacaoAoVivo;
}
