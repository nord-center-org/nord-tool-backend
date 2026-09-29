package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoriaHistoricoDto {

    private Integer nrVersao;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dtAlteracao;
    private String nmUsuario;
    private List<ApartamentoVistoriaHistoricoAlteradoDto> lsApartamentoVistoriaAlterado;

    public static ApartamentoVistoriaHistoricoDto converter(
            List<ApartamentoVistoriaHistoricoConsultaDto> lsApartamentoVistoriaHistoricoConsultaDto) {

        ApartamentoVistoriaHistoricoConsultaDto apartamentoVistoriaHistoricoConsultaDto =
                lsApartamentoVistoriaHistoricoConsultaDto.stream()
                        .findFirst()
                        .orElse(null);

        return ApartamentoVistoriaHistoricoDto.builder()
                .nrVersao(apartamentoVistoriaHistoricoConsultaDto.getNrVersao())
                .dtAlteracao(apartamentoVistoriaHistoricoConsultaDto.getDtAlteracao())
                .nmUsuario(apartamentoVistoriaHistoricoConsultaDto.getNmUsuario())
                .lsApartamentoVistoriaAlterado(
                        lsApartamentoVistoriaHistoricoConsultaDto.stream()
                                .map(ApartamentoVistoriaHistoricoAlteradoDto::converter)
                                .collect(Collectors.toList())
                )
                .build();
    }

}
