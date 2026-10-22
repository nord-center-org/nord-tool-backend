package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Um treino feito: data, duração e a carga usada em cada exercício. */
@Data
public class TreinoSessaoDto {
    private Long id;
    private Long idTreino;
    private String nmTreino;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dtSessao;
    private Integer nrDuracaoMin;
    private String txObservacao;
    private List<TreinoSessaoExercicioDto> exercicios = new ArrayList<>();
}
