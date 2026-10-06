package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoriaDto {

    private Long idApartamentoVistoria;
    private String nmApartamentoVistoria;
    private Integer idDiaSemana;
    private String nmDiaSemana;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtApartamentoVigente;
    private String nmHorarioVistoria;
    private Integer idStatusVistoria;
    private String nmStatusVistoria;
    private boolean inMarcarRevistoria;
    private String txObservacaoRevistoria;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private LocalDate dtRevistoriaVigente;

    /** Dados do ÚLTIMO termo de reprova do apartamento (agregados, sem bytes). */
    private boolean inTermoAnexado;
    private Integer qtTermos;
    private Integer nrUltimoTermo;
    /** PENDENTE | EM_ANDAMENTO | CONCLUIDO (nulo quando não há termo). */
    private String nmSituacaoTermo;
    private Integer qtFotosTermo;
    private Integer nrPaginasTermo;
    private Integer nrPaginasComFoto;

    public static ApartamentoVistoriaDto converterToDto(ApartamentoVistoria apartamentoVistoria ) {
        return ApartamentoVistoriaDto.builder()
                .idApartamentoVistoria(apartamentoVistoria.getId())
                .nmApartamentoVistoria(apartamentoVistoria.getNmApartamentoVistoria())
                .idDiaSemana(apartamentoVistoria.getIdDiaSemana())
                .nmDiaSemana(apartamentoVistoria.getNmDiaSemana())
                .dtApartamentoVigente(apartamentoVistoria.getDtApartamentoVigente())
                .nmHorarioVistoria(apartamentoVistoria.getNmHorarioVistoria())
                .idStatusVistoria(apartamentoVistoria.getIdStatusVistoria())
                .nmStatusVistoria(apartamentoVistoria.getNmStatusVistoria())
                .inMarcarRevistoria(apartamentoVistoria.isInMarcarRevistoria())
                .txObservacaoRevistoria(apartamentoVistoria.getTxObservacaoRevistoria())
                .dtRevistoriaVigente(apartamentoVistoria.getDtRevistoriaVigente())
                .inTermoAnexado(apartamentoVistoria.isInTermoAnexado())
                .qtTermos(apartamentoVistoria.getQtTermos())
                .nrUltimoTermo(apartamentoVistoria.getNrUltimoTermo())
                .nmSituacaoTermo(apartamentoVistoria.getNmSituacaoTermo())
                .qtFotosTermo(apartamentoVistoria.getQtFotosTermo())
                .nrPaginasTermo(apartamentoVistoria.getNrPaginasTermo())
                .nrPaginasComFoto(apartamentoVistoria.getNrPaginasComFoto())
                .build();
    }
}
