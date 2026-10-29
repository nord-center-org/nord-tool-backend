package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Mudanças de status na grade da escadinha (uma ou várias células de uma vez). */
@Data
public class EscadinhaCelulasForm {
    @NotNull
    @Size(min = 1, max = 500)
    @Valid
    private List<Celula> alteracoes = new ArrayList<>();

    @Data
    public static class Celula {
        /** "idPavimento|lado|idAtividade". */
        private String chave;
        /** E = executado, X = em execução, N = não se aplica; vazio ou nulo limpa a célula. */
        private String status;
        /** Data da execução (AAAA-MM-DD); sem ela vale hoje. */
        private String data;
    }
}
