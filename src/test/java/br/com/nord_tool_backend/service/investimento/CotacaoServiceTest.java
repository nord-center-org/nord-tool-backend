package br.com.nord_tool_backend.service.investimento;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CotacaoServiceTest {

    private static final class RelogioMutavel extends Clock {
        Instant agora = Instant.parse("2026-10-08T12:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void cacheEvitaChamadasDentroDoTtlEBuscaDepois() {
        AtomicInteger chamadas = new AtomicInteger();
        CotacaoProvider provider = new CotacaoProvider() {
            @Override public Optional<Cotacao> cotar(String t) { chamadas.incrementAndGet(); return Optional.of(new Cotacao(new BigDecimal("10.00"), Instant.now())); }
            @Override public java.util.List<ProventoCotado> proventos(String t) { return Collections.emptyList(); }
        };
        RelogioMutavel relogio = new RelogioMutavel();
        CotacaoService service = new CotacaoService(provider, relogio, 60);
        assertEquals(1, service.cotar(Collections.singleton("HGLG11")).size());
        service.cotar(Collections.singleton("HGLG11"));
        assertEquals(1, chamadas.get());
        relogio.agora = relogio.agora.plusSeconds(61);
        service.cotar(Collections.singleton("HGLG11"));
        assertEquals(2, chamadas.get());
    }

    @Test
    void tickerSemCotacaoFicaForaDoResultado() {
        CotacaoProvider provider = new CotacaoProvider() {
            @Override public Optional<Cotacao> cotar(String t) { return Optional.empty(); }
            @Override public java.util.List<ProventoCotado> proventos(String t) { return Collections.emptyList(); }
        };
        assertTrue(new CotacaoService(provider, new RelogioMutavel(), 60).cotar(Collections.singleton("XPML11")).isEmpty());
    }
}
