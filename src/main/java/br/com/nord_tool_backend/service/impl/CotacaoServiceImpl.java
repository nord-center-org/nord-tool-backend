package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.CotacaoService;
import br.com.nord_tool_backend.service.investimento.Cotacao;
import br.com.nord_tool_backend.service.investimento.CotacaoProvider;
import br.com.nord_tool_backend.service.investimento.ProventoCotado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Cotações com cache curto: várias telas abertas não multiplicam as chamadas ao provedor gratuito. */
@Service
public class CotacaoServiceImpl implements CotacaoService {

    private static final class Entrada {
        final Optional<Cotacao> cotacao;
        final Instant busca;

        Entrada(Optional<Cotacao> cotacao, Instant busca) {
            this.cotacao = cotacao;
            this.busca = busca;
        }
    }

    private final CotacaoProvider provider;
    private final Clock clock;
    private final Duration ttl;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public CotacaoServiceImpl(CotacaoProvider provider, Clock clock,
                              @Value("${nord-tool.cotacao.ttl-seconds:60}") long ttlSegundos) {
        this.provider = provider;
        this.clock = clock;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    @Override
    public Map<String, Cotacao> cotar(Set<String> tickers) {
        Instant agora = clock.instant();
        Map<String, Cotacao> resultado = new HashMap<>();
        for (String ticker : tickers) {
            Entrada e = cache.get(ticker);
            if (e == null || Duration.between(e.busca, agora).compareTo(ttl) >= 0) {
                e = new Entrada(provider.cotar(ticker), agora);
                cache.put(ticker, e);
            }
            e.cotacao.ifPresent(c -> resultado.put(ticker, c));
        }
        return resultado;
    }

    @Override
    public java.util.List<ProventoCotado> proventos(String ticker) {
        return provider.proventos(ticker);
    }
}
