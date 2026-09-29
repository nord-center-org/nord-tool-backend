package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoriaHistoricoAlteradoDto {
    private String nmAtributo;
    private String txAnterior;
    private String txAtual;

    public static ApartamentoVistoriaHistoricoAlteradoDto converter(
            ApartamentoVistoriaHistoricoConsultaDto apartamentoVistoriaHistoricoConsultaDto) {
        return ApartamentoVistoriaHistoricoAlteradoDto.builder()
                .nmAtributo(apartamentoVistoriaHistoricoConsultaDto.getNmAtributo())
                .txAnterior(apartamentoVistoriaHistoricoConsultaDto.getTxAnterior())
                .txAtual(apartamentoVistoriaHistoricoConsultaDto.getTxAtual())
                .build();
    }
}
