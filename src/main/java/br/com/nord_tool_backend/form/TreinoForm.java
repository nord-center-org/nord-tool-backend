package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class TreinoForm {
    @NotBlank(message = "Informe o nome do treino")
    @Size(max = 80, message = "O nome do treino deve ter no máximo 80 caracteres")
    private String nmTreino;

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    private String txObservacao;

    @Pattern(regexp = "^$|^[1-7](,[1-7]){0,6}$", message = "Dias: números de 1 (segunda) a 7 (domingo) separados por vírgula")
    private String txDias;

    private Integer nrOrdem;
    private Boolean inAtivo;

    /** Obrigatória na edição. */
    private Integer nrVersao;

    /** Lista completa; substitui os exercícios (os que saem da lista são excluídos). */
    @Valid
    @Size(max = 40, message = "O treino pode ter no máximo 40 exercícios")
    private List<TreinoExercicioForm> exercicios = new ArrayList<>();
}
