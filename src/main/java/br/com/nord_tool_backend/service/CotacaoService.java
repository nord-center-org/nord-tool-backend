package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.service.investimento.Cotacao;
import br.com.nord_tool_backend.service.investimento.ProventoCotado;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cotações com cache curto: várias telas abertas não multiplicam as chamadas ao provedor gratuito. */
public interface CotacaoService {

    /** Só os tickers que o provedor soube cotar agora (ou há menos de um TTL). */
    Map<String, Cotacao> cotar(Set<String> tickers);

    List<ProventoCotado> proventos(String ticker);
}
