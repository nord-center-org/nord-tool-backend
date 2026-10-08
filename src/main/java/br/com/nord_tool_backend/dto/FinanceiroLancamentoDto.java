package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroLancamentoDto {
    private Long idLancamento;
    /** Mês a que o lançamento pertence, no formato yyyy-MM. */
    private String competencia;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtLancamento;
    private Long idCategoria;
    private String nmCategoria;
    /** ENTRADA | SAIDA (vem da categoria). */
    private String cdTipo;
    private Long idPessoa;
    private String nmPessoa;
    private String dsLancamento;
    private BigDecimal vlLancamento;
    private boolean inRealizado;
    private Integer nrParcela;
    private Integer qtParcela;
    /** Quem digitou o lançamento (o login), diferente de "de quem é" (idPessoa). */
    private Long idUsuarioCriacao;
    private String nmUsuarioCriacao;
    /** Revisão para controle de concorrência: devolver na edição/marcação/exclusão. */
    private Integer nrVersao;

    public static FinanceiroLancamentoDto de(FinanceiroLancamento l) {
        String competencia = l.getDtCompetencia() == null ? null
                : String.format("%04d-%02d", l.getDtCompetencia().getYear(), l.getDtCompetencia().getMonthValue());
        return new FinanceiroLancamentoDto(l.getId(), competencia, l.getDtLancamento(), l.getIdCategoria(),
                l.getNmCategoria(), l.getCdTipo(), l.getIdPessoa(), l.getNmPessoa(), l.getDsLancamento(),
                l.getVlLancamento(), Boolean.TRUE.equals(l.getInRealizado()), l.getNrParcela(), l.getQtParcela(),
                l.getIdUsuarioCriacao(), l.getNmUsuarioCriacao(), l.getNrVersao());
    }
}
