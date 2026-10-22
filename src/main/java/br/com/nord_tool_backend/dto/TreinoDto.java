package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Ficha de treino do usuário com os exercícios. */
@Data
public class TreinoDto {
    private Long id;
    private String nmTreino;
    private String txObservacao;
    /** Dias sugeridos (1 = segunda … 7 = domingo), separados por vírgula. */
    private String txDias;
    private Integer nrOrdem;
    private Boolean inAtivo;
    private Integer nrVersao;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dtUltimaSessao;
    private Long qtSessoes;
    private List<TreinoExercicioDto> exercicios = new ArrayList<>();
}
