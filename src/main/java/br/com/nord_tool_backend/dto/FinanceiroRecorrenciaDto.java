package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroRecorrenciaDto {
    private Long idRecorrencia;
    private Long idCategoria;
    private String nmCategoria;
    private String cdTipo;
    private Long idPessoa;
    private String nmPessoa;
    private String dsRecorrencia;
    private BigDecimal vlRecorrencia;
    private Integer nrDia;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtInicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtFim;
    private boolean inAtivo;
    private Integer nrVersao;

    public static FinanceiroRecorrenciaDto de(FinanceiroRecorrencia r) {
        return new FinanceiroRecorrenciaDto(r.getId(), r.getIdCategoria(), r.getNmCategoria(), r.getCdTipo(), r.getIdPessoa(),
                r.getNmPessoa(), r.getDsRecorrencia(), r.getVlRecorrencia(), r.getNrDia(), r.getDtInicio(), r.getDtFim(),
                !Boolean.FALSE.equals(r.getInAtivo()), r.getNrVersao());
    }
}
