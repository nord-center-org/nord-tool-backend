package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class GamePartidaForm {
    @NotNull(message = "Informe o jogo")
    @Pattern(regexp = "^[a-z0-9-]{1,40}$", message = "Jogo inválido")
    private String cdJogo;
    @NotNull(message = "Informe o modo")
    @Pattern(regexp = "^[a-z0-9-]{1,40}$", message = "Modo inválido")
    private String cdModo;
    @Min(value = 0, message = "Pontos inválidos")
    @Max(value = 10_000_000, message = "Pontos inválidos")
    private int nrPontos;
    private boolean inVitoria;
    @Min(value = 1, message = "Colocação inválida")
    @Max(value = 8, message = "Colocação inválida")
    private int nrColocacao = 1;
    @Min(value = 1, message = "Jogadores inválidos")
    @Max(value = 8, message = "No máximo 8 jogadores")
    private int nrJogadores = 1;
    @Min(value = 0, message = "Bots inválidos")
    @Max(value = 7, message = "Bots inválidos")
    private int nrBots;
    @Pattern(regexp = "^(facil|medio|dificil)$", message = "Dificuldade inválida")
    private String cdDificuldade;
    @Min(value = 0, message = "Duração inválida")
    @Max(value = 86_400, message = "Duração inválida")
    private int nrDuracaoS;
    private Long idCopa;
}
