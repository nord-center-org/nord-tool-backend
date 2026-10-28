package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Relatório do cronograma pessoal para assistentes de IA: hoje, atrasadas, próximos dias e sem data. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioCronogramaDto {
    private String geradoEm;
    private String pessoa;
    private DiaDto hoje;
    private List<ItemDto> atrasadas;
    private List<DiaDto> proximosDias;
    private List<ItemDto> semData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaDto {
        private String data;
        private String diaSemana;
        private List<ItemDto> itens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private String titulo;
        /** fixo (rotina da semana), agendado (data e hora marcadas) ou prazo (vence no dia). */
        private String tipo;
        private String horario;
        private String categoria;
        private String status;
        private String prazo;
        private String tag;
        private String observacao;
    }
}
