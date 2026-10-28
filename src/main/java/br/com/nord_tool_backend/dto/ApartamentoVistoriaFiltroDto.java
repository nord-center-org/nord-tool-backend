package br.com.nord_tool_backend.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoriaFiltroDto {
    private Long idApartamentoVistoria;
    private String nmApartamentoVistoria;
    private String nmDiaSemana;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private String dtApartamentoVigente;
    private String nmHorarioVistoria;
    private String nmStatusVistoria;
    private String txObservacaoRevistoria;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
    private String dtRevistoriaVigente;
}
