package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroConfiguracaoDto {
    /** Saldo mínimo desejado ao fim do mês (o "verde" do dashboard). */
    private BigDecimal vlMetaSaldo;
    /** Quantos meses anteriores entram na média das projeções. */
    private Integer nrMesesMedia;
    /** Dia do mês em que os valores reais costumam ser conferidos. */
    private Integer nrDiaConferencia;
    /** Dia do mês em que a fatura do cartão fecha. */
    private Integer nrDiaFechamentoFatura;

    public static FinanceiroConfiguracaoDto de(FinanceiroConfiguracao c) {
        return new FinanceiroConfiguracaoDto(c.getVlMetaSaldo(), c.getNrMesesMedia(), c.getNrDiaConferencia(), c.getNrDiaFechamentoFatura());
    }
}
