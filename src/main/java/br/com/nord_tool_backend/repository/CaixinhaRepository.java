package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import br.com.nord_tool_backend.domain.CaixinhaResponsavel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CaixinhaRepository {

    // responsáveis
    List<CaixinhaResponsavel> listarResponsaveis();
    Optional<CaixinhaResponsavel> buscarResponsavel(Long id);
    Long inserirResponsavel(CaixinhaResponsavel r);
    void alterarResponsavel(CaixinhaResponsavel r);

    // lançamentos
    List<CaixinhaLancamento> listarLancamentos(CaixinhaFiltro filtro);
    Optional<CaixinhaLancamento> buscarLancamento(Long id);
    /** Id do lançamento já criado com este cdRequisicao, se houver. */
    Optional<Long> buscarLancamentoPorRequisicao(String cdRequisicao);
    /** Insere e devolve o id; vazio se o cdRequisicao já existia (nada é gravado). */
    Optional<Long> inserirLancamento(CaixinhaLancamento l);
    /** Devolve quantas linhas foram alteradas (0 = versão diferente ou inexistente). */
    int alterarLancamento(CaixinhaLancamento l, int nrVersao);
    int marcarLancamento(Long id, boolean lancado, boolean pago, int nrVersao);
    int deletarLancamento(Long id, int nrVersao);

    /** Total, pago, quantidade e quantidade de pagos dos lançamentos filtrados. */
    Totais resumir(CaixinhaFiltro filtro);

    // comprovantes
    List<CaixinhaComprovante> listarComprovantes(Long idLancamento);
    Optional<CaixinhaComprovante> buscarComprovante(Long id);
    Optional<Long> buscarComprovantePorRequisicao(String cdRequisicao);
    Long inserirComprovante(Long idLancamento, Long idArquivo, String cdRequisicao);
    void deletarComprovante(Long id);

    class Totais {
        public final BigDecimal total;
        public final BigDecimal pago;
        public final int qtLancamentos;
        public final int qtPagos;

        public Totais(BigDecimal total, BigDecimal pago, int qtLancamentos, int qtPagos) {
            this.total = total;
            this.pago = pago;
            this.qtLancamentos = qtLancamentos;
            this.qtPagos = qtPagos;
        }
    }
}
