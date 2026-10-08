package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroFaturaLeitura;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acesso a dados da conta do mês: configuração, fechamento, histórico por categoria, faturas e recorrências. */
public interface FinanceiroProjecaoRepository {

    // configuração
    Optional<FinanceiroConfiguracao> buscarConfiguracao();
    /** Cria o registro padrão se ainda não existir. */
    void garantirConfiguracao();
    void alterarConfiguracao(FinanceiroConfiguracao c);

    // meses (competência sempre no dia 1)
    Optional<FinanceiroMes> buscarMes(LocalDate competencia);
    List<FinanceiroMes> listarMeses(LocalDate de, LocalDate ate);
    void garantirMes(LocalDate competencia);
    /** Devolve quantas linhas foram alteradas (0 = mês já fechado). */
    int definirSaldoInicial(LocalDate competencia, BigDecimal vlSaldoInicial);
    /** Devolve quantas linhas foram alteradas (0 = mês já fechado). */
    int fecharMes(LocalDate competencia, BigDecimal vlSaldoFinal, Long idUsuario);
    /** Devolve quantas linhas foram alteradas (0 = mês não estava fechado). */
    int reabrirMes(LocalDate competencia);
    boolean existeMesFechadoApos(LocalDate competencia);
    /** Primeiro mês com lançamento ou registro de mês; nulo se não há nada. */
    Optional<LocalDate> primeiraCompetencia();
    int contarPrevistos(LocalDate competencia);

    /** Soma e quantidade por mês e categoria; {@code idPessoa} nulo = todas as pessoas. */
    List<FinanceiroSomaMes> somarPorCategoria(LocalDate de, LocalDate ate, Long idPessoa);

    // faturas
    List<FinanceiroFaturaAberta> faturasDoMes(LocalDate competencia, Long idPessoa);
    void registrarLeitura(Long idLancamento, LocalDate dtLeitura, BigDecimal vlLeitura, Long idUsuario);
    List<FinanceiroFaturaLeitura> listarLeituras(Long idLancamento);

    // recorrências
    List<FinanceiroRecorrencia> listarRecorrencias();
    Optional<FinanceiroRecorrencia> buscarRecorrencia(Long id);
    Long inserirRecorrencia(FinanceiroRecorrencia r);
    /** Devolve quantas linhas foram alteradas (0 = versão diferente ou inexistente). */
    int alterarRecorrencia(FinanceiroRecorrencia r, int nrVersao);
}
