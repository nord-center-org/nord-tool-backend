package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class CaixinhaLancamentoDto {
    private Long idLancamento;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtLancamento;
    private Long idResponsavel;
    private String nmResponsavel;
    private String txInsumo;
    private BigDecimal vlValor;
    private boolean inLancado;
    private boolean inPago;
    /** Revisão para controle de concorrência: devolver na edição/marcação/exclusão. */
    private Integer nrVersao;
    private Integer qtComprovantes;

    public static CaixinhaLancamentoDto de(CaixinhaLancamento l) {
        return new CaixinhaLancamentoDto(l.getId(), l.getDtLancamento(), l.getIdResponsavel(), l.getNmResponsavel(),
                l.getTxInsumo(), l.getVlValor(), Boolean.TRUE.equals(l.getInLancado()), Boolean.TRUE.equals(l.getInPago()),
                l.getNrVersao(), l.getQtComprovantes() == null ? 0 : l.getQtComprovantes());
    }
}
