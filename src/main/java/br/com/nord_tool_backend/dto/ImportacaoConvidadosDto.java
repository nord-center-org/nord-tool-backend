package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Relatório da importação de convidados por planilha. */
@Data @AllArgsConstructor @NoArgsConstructor
public class ImportacaoConvidadosDto {
    private int importados;
    private List<LinhaRejeitada> rejeitados = new ArrayList<>();

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class LinhaRejeitada {
        /** Número da linha na planilha (1 = primeira linha, o cabeçalho). */
        private int linha;
        private String motivo;
    }
}
