package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lançamento de fatura de um mês com a data da última leitura (nula se nunca foi atualizado). */
@Getter @Setter @NoArgsConstructor
public class FinanceiroFaturaAberta {
    private Long idLancamento;
    private Long idCategoria;
    private BigDecimal vlLancamento;
    private LocalDate dtLeitura;
}
