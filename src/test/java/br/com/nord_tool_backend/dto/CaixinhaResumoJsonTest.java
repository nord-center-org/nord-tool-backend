package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O frontend lê exatamente estes nomes; o Lombok + Jackson já gerou "apagar" no lugar de "aPagar". */
class CaixinhaResumoJsonTest {
    @Test
    void resumoSerializaOsNomesQueOFrontendLe() throws Exception {
        String json = new ObjectMapper().writeValueAsString(
                new CaixinhaResumoDto(BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), 2, 1, 1));
        for (String campo : new String[]{"total", "pago", "aPagar", "qtLancamentos", "qtPagos", "qtPendentes"}) {
            assertTrue(json.contains("\"" + campo + "\":"), "falta o campo " + campo + " em " + json);
        }
        assertFalse(json.contains("\"apagar\""), json);
    }

    @Test
    void lancamentoSerializaOsNomesQueOFrontendLe() throws Exception {
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(
                new CaixinhaLancamentoDto(1L, java.time.LocalDate.of(2026, 10, 1), 1L, "Ana", "x", BigDecimal.TEN, true, false, 1, 0));
        for (String campo : new String[]{"idLancamento", "dtLancamento", "idResponsavel", "nmResponsavel", "txInsumo", "vlValor",
                "inLancado", "inPago", "nrVersao", "qtComprovantes"}) {
            assertTrue(json.contains("\"" + campo + "\":"), "falta o campo " + campo + " em " + json);
        }
        assertTrue(json.contains("\"dtLancamento\":\"01/10/2026\""), json);
    }
}
