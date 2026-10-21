package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Medicao;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class MedicaoDto {
    private Long id;
    private Integer nrMedicao;
    private String cdContrato;
    private String nmRazaoSocial;
    private String nrContrato;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dtMedicao;
    private BigDecimal vlMedicao;
    private String txMemorial;
    private String cdStatus;
    private String nmStatus;
    /** Revisão para controle de concorrência: devolver na edição/troca de status/exclusão. */
    private Integer nrVersao;

    public static MedicaoDto de(Medicao m) {
        return new MedicaoDto(m.getId(), m.getNrMedicao(), m.getCdContrato(), m.getNmRazaoSocial(), m.getNrContrato(),
                m.getDtMedicao(), m.getVlMedicao(), m.getTxMemorial(), m.getCdStatus(), m.getNmStatus(), m.getNrVersao());
    }
}
