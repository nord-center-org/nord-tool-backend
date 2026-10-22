package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Registro de um treino feito. Com atualizarCargas, a carga usada vira a carga atual da ficha. */
@Data
public class TreinoSessaoForm {
    @NotNull(message = "Informe o treino")
    private Long idTreino;

    /** yyyy-MM-dd */
    @NotNull(message = "Informe a data")
    private LocalDate dtSessao;

    @Min(value = 0, message = "Duração: de 0 a 600 minutos")
    @Max(value = 600, message = "Duração: de 0 a 600 minutos")
    private Integer nrDuracaoMin;

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    private String txObservacao;

    private Boolean atualizarCargas;

    @Valid
    @Size(max = 40, message = "No máximo 40 exercícios por sessão")
    private List<Item> exercicios = new ArrayList<>();

    @Data
    public static class Item {
        private Long idExercicio;

        @NotBlank(message = "Informe o exercício")
        @Size(max = 100, message = "O exercício deve ter no máximo 100 caracteres")
        private String nmExercicio;

        @Size(max = 20, message = "Repetições: no máximo 20 caracteres")
        private String txRepeticoes;

        @DecimalMin(value = "0", message = "A carga não pode ser negativa")
        @Digits(integer = 5, fraction = 2, message = "Carga inválida")
        private BigDecimal vlCarga;

        private Boolean inFeito;
    }
}
