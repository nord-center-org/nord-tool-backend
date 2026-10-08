package br.com.nord_tool_backend.service.investimento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiCotacaoProviderTest {

    private MockRestServiceServer servidor;
    private BrapiCotacaoProvider provider;

    @BeforeEach
    void setUp() {
        RestTemplate http = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(http).build();
        provider = new BrapiCotacaoProvider(http, "https://brapi.test/api/", "segredo");
    }

    @Test
    void leOPrecoEOMomento() {
        servidor.expect(requestTo("https://brapi.test/api/quote/HGLG11")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer segredo"))
                .andRespond(withSuccess("{\"results\":[{\"symbol\":\"HGLG11\",\"regularMarketPrice\":158.42,\"regularMarketTime\":\"2026-10-08T17:59:00.000Z\"}]}", MediaType.APPLICATION_JSON));
        Optional<Cotacao> c = provider.cotar("HGLG11");
        assertTrue(c.isPresent());
        assertEquals(new BigDecimal("158.42"), c.get().getPreco());
        assertEquals(Instant.parse("2026-10-08T17:59:00Z"), c.get().getMomento());
    }

    @Test
    void falhaDoProvedorViraVazio() {
        servidor.expect(requestTo("https://brapi.test/api/quote/XPML11")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertFalse(provider.cotar("XPML11").isPresent());
    }

    @Test
    void respostaSemPrecoOuSemResultadoViraVazio() {
        servidor.expect(requestTo("https://brapi.test/api/quote/AAAA11")).andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://brapi.test/api/quote/BBBB11")).andRespond(withSuccess("{\"results\":[{\"symbol\":\"BBBB11\"}]}", MediaType.APPLICATION_JSON));
        assertFalse(provider.cotar("AAAA11").isPresent());
        assertFalse(provider.cotar("BBBB11").isPresent());
    }

    @Test
    void leOsProventosIgnorandoOsIncompletos() {
        servidor.expect(requestTo("https://brapi.test/api/quote/HGLG11?dividends=true")).andRespond(withSuccess(
                "{\"results\":[{\"dividendsData\":{\"cashDividends\":["
                        + "{\"paymentDate\":\"2026-10-15T00:00:00.000Z\",\"rate\":1.10,\"lastDatePrior\":\"2026-09-30T00:00:00.000Z\"},"
                        + "{\"paymentDate\":\"2026-09-15T00:00:00.000Z\",\"rate\":1.05,\"approvedOn\":\"2026-08-29T00:00:00.000Z\"},"
                        + "{\"paymentDate\":\"2026-08-15T00:00:00.000Z\",\"lastDatePrior\":\"2026-07-31T00:00:00.000Z\"},"
                        + "{\"rate\":1.0,\"lastDatePrior\":\"2026-06-30T00:00:00.000Z\"}]}}]}", MediaType.APPLICATION_JSON));
        List<ProventoCotado> p = provider.proventos("HGLG11");
        assertEquals(2, p.size());
        assertEquals("2026-09-30", p.get(0).getDtCom().toString());
        assertEquals("2026-10-15", p.get(0).getDtPagamento().toString());
        assertEquals(0, new BigDecimal("1.10").compareTo(p.get(0).getValorPorCota()));
        assertEquals("2026-08-29", p.get(1).getDtCom().toString());
    }
}
