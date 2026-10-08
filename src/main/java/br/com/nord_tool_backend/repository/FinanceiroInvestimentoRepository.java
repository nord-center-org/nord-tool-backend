package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroProvento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FinanceiroInvestimentoRepository {

    List<FinanceiroAtivo> listarAtivos(Long idPessoa);
    Optional<FinanceiroAtivo> buscarAtivo(Long id);
    Optional<FinanceiroAtivo> buscarAtivoPorTicker(String cdTicker, Long idPessoa);
    Long inserirAtivo(FinanceiroAtivo a);
    /** 0 = a versão mudou. */
    int alterarAtivo(FinanceiroAtivo a, int nrVersao);
    void atualizarCotacao(Long idAtivo, BigDecimal vlCotacao, LocalDateTime dhCotacao);

    List<FinanceiroOperacao> listarOperacoes(Collection<Long> idsAtivo);
    Optional<FinanceiroOperacao> buscarOperacao(Long id);
    Optional<Long> buscarAtivoDaRequisicao(String cdRequisicao);
    /** Vazio = a requisição já tinha sido gravada. */
    Optional<Long> inserirOperacao(FinanceiroOperacao o);
    void excluirOperacao(Long id);

    List<FinanceiroProvento> listarProventos(Collection<Long> idsAtivo);
    Optional<FinanceiroProvento> buscarProvento(Long id);
    void gravarProventoManual(FinanceiroProvento p);
    /** @return true se gravou (novo ou atualizando um importado). */
    boolean gravarProventoImportado(FinanceiroProvento p);
    void excluirProvento(Long id);
}
