package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class GameCopaForm {
    @Min(value = 3, message = "A Copa tem 3, 5 ou 7 rodadas")
    @Max(value = 7, message = "A Copa tem 3, 5 ou 7 rodadas")
    private int nrRodadas;
    @Min(value = 1, message = "Colocação inválida")
    @Max(value = 8, message = "Colocação inválida")
    private int nrColocacao = 1;
    /** O resultado (participantes, pontos por rodada e pódio) em JSON, montado pela tela. */
    @NotBlank(message = "Informe o resultado da Copa")
    @Size(max = 20_000, message = "Resultado da Copa muito grande")
    private String txResultado;
}
