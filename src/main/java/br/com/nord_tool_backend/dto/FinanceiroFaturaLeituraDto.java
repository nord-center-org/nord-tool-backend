package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroFaturaLeitura;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroFaturaLeituraDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtLeitura;
    private BigDecimal vlLeitura;

    public static FinanceiroFaturaLeituraDto de(FinanceiroFaturaLeitura l) {
        return new FinanceiroFaturaLeituraDto(l.getDtLeitura(), l.getVlLeitura());
    }
}
