package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Montagem dos filtros do extrato (sem banco): valores sempre por parâmetro, nunca concatenados no SQL. */
class FinanceiroRepositoryImplTest {

    @Test
    void semFiltroNaoAcrescentaCondicoes() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        assertEquals("", FinanceiroRepositoryImpl.condicoes(null, params));
        assertEquals("", FinanceiroRepositoryImpl.condicoes(new FinanceiroFiltro(), params));
        assertEquals("", FinanceiroRepositoryImpl.condicoes(
                new FinanceiroFiltro(" ", null, null, null, null, " ", "TODOS", "  "), params));
        assertTrue(params.getParameterNames().length == 0);
    }

    @Test
    void todosOsFiltrosViramCondicoesComParametros() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = FinanceiroRepositoryImpl.condicoes(new FinanceiroFiltro("2026-10", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), 2L, 3L, "saida", "previsto", "luz"), params);

        assertTrue(sql.contains("l.dt_competencia = :competencia"));
        assertTrue(sql.contains("l.dt_lancamento >= :de"));
        assertTrue(sql.contains("l.dt_lancamento <= :ate"));
        assertTrue(sql.contains("l.id_pessoa = :idPessoa"));
        assertTrue(sql.contains("l.id_categoria = :idCategoria"));
        assertTrue(sql.contains("c.cd_tipo = :tipo"));
        assertTrue(sql.contains("l.in_realizado = FALSE"));
        assertTrue(sql.contains("ILIKE :texto"));
        assertEquals(Date.valueOf(LocalDate.of(2026, 10, 1)), params.getValue("competencia"));
        assertEquals("SAIDA", params.getValue("tipo"));
        assertEquals("%luz%", params.getValue("texto"));
    }

    @Test
    void situacaoRealizadoFiltraPeloIndicador() {
        String sql = FinanceiroRepositoryImpl.condicoes(
                new FinanceiroFiltro(null, null, null, null, null, null, "REALIZADO", null), new MapSqlParameterSource());
        assertTrue(sql.contains("l.in_realizado = TRUE"));
    }

    @Test
    void textoDigitadoNuncaEntraNoSql() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = FinanceiroRepositoryImpl.condicoes(
                new FinanceiroFiltro(null, null, null, null, null, null, null, "x'; DROP TABLE financeiro_lancamento; --"), params);

        assertFalse(sql.contains("DROP"));
        assertTrue(String.valueOf(params.getValue("texto")).contains("DROP TABLE"));
    }

    @Test
    void percentUnderscoreEBarraDoTextoValemComoLetras() {
        assertEquals("100\\%", FinanceiroRepositoryImpl.escaparLike("100%"));
        assertEquals("a\\_b", FinanceiroRepositoryImpl.escaparLike("a_b"));
        assertEquals("c\\\\d", FinanceiroRepositoryImpl.escaparLike("c\\d"));
    }
}
