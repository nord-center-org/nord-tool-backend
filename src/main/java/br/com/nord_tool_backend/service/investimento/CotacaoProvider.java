package br.com.nord_tool_backend.service.investimento;

import java.util.List;
import java.util.Optional;

/** Fonte de cotações e proventos. Trocar de provedor é implementar esta interface; falhas viram resposta vazia. */
public interface CotacaoProvider {

    /** Cotação atual do ticker, ou vazio se o provedor não souber/estiver fora do ar. */
    Optional<Cotacao> cotar(String ticker);

    /** Proventos conhecidos do ticker (lista vazia se indisponível). */
    List<ProventoCotado> proventos(String ticker);
}
