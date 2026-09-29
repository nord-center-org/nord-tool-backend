package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InfoGeralApartamentoVistoriaDto {
    private String nmStatusVistoria;
    private Integer qtApartamentoStatusVistoria;
    private Double pcApartamentoStatusVistoria;
    private Integer nrTotalRegistros;

    public static InfoGeralApartamentoVistoriaDto converterToDTO(InfoGeralApartamentoVistoria infoGeralApartamentoVistoria) {
        return InfoGeralApartamentoVistoriaDto.builder()
                .nmStatusVistoria(infoGeralApartamentoVistoria.getNmStatusVistoria())
                .qtApartamentoStatusVistoria(infoGeralApartamentoVistoria.getQtApartamentoStatusVistoria())
                .pcApartamentoStatusVistoria(infoGeralApartamentoVistoria.getPcApartamentoStatusVistoria())
                .nrTotalRegistros(infoGeralApartamentoVistoria.getNrTotalRegistros())
                .build();
    }
}
