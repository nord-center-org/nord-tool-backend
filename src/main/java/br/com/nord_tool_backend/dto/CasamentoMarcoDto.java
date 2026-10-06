package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CasamentoMarco;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoMarcoDto {
    private Long idMarco;
    private String nmTitulo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtPrazo;
    private boolean inConcluido;
    private String txObservacao;

    public static CasamentoMarcoDto de(CasamentoMarco m) {
        return new CasamentoMarcoDto(m.getId(), m.getNmTitulo(), m.getDtPrazo(),
                Boolean.TRUE.equals(m.getInConcluido()), m.getTxObservacao());
    }
}
