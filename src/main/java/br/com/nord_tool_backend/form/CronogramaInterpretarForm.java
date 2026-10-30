package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Pedido em texto livre para a Lugia virar demanda(s) do cronograma. */
@Data
public class CronogramaInterpretarForm {
    @NotBlank(message = "Escreva o que você quer programar.")
    @Size(max = 2000, message = "Escreva em até 2000 caracteres.")
    private String texto;
    /** Pedidos anteriores do mesmo chat (o atual pode ajustar o que já foi pedido). */
    @Size(max = 10)
    private List<@Size(max = 2000) String> anteriores = new ArrayList<>();
    /** Data de hoje na tela (AAAA-MM-DD), para "amanhã", "sexta que vem"... Sem ela, a data do servidor. */
    private String hoje;
}
