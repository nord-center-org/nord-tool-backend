package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Controle de finalização do DAT. Universo: apartamentos com status "Reprovado" ou que já têm termo.
 * A situação considerada é a do ÚLTIMO termo de cada apartamento.
 */
@Data @AllArgsConstructor @NoArgsConstructor
public class TermoReprovaResumoGeralDto {
    private Integer totalApartamentosComReprova;
    private Integer comTermo;
    private Integer semTermo;
    private Integer concluidos;
    private Integer emAndamento;
    private Integer pendentes;
    /** concluidos / total × 100, com uma casa decimal (0 quando não há apartamentos). */
    private Double percentualConcluido;
}
