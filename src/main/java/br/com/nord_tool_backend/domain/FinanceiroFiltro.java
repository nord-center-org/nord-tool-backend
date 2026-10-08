package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Filtros opcionais do extrato e do resumo; todos combinam com AND.
 * {@code competencia} = yyyy-MM; {@code de}/{@code ate} filtram a data do lançamento;
 * {@code tipo} = ENTRADA | SAIDA; {@code situacao} = TODOS | REALIZADO | PREVISTO.
 */
@Getter @NoArgsConstructor @AllArgsConstructor
public class FinanceiroFiltro {
    private String competencia;
    private LocalDate de;
    private LocalDate ate;
    private Long idPessoa;
    private Long idCategoria;
    private String tipo;
    private String situacao;
    private String texto;

    /** Dia 1 da competência pedida, ou nulo quando não há filtro de mês. Lança DateTimeParseException se inválida. */
    public LocalDate competenciaData() {
        if (competencia == null || competencia.trim().isEmpty()) return null;
        return YearMonth.parse(competencia.trim()).atDay(1);
    }
}
