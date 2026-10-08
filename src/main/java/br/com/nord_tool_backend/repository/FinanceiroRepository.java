package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FinanceiroRepository {

    // pessoas
    List<FinanceiroPessoa> listarPessoas();
    Optional<FinanceiroPessoa> buscarPessoa(Long id);
    Long inserirPessoa(FinanceiroPessoa p);
    void alterarPessoa(FinanceiroPessoa p);

    // categorias
    List<FinanceiroCategoria> listarCategorias();
    Optional<FinanceiroCategoria> buscarCategoria(Long id);
    Long inserirCategoria(FinanceiroCategoria c);
    void alterarCategoria(FinanceiroCategoria c);
    int contarLancamentosDaCategoria(Long idCategoria);

    // lançamentos
    List<FinanceiroLancamento> listarLancamentos(FinanceiroFiltro filtro);
    Optional<FinanceiroLancamento> buscarLancamento(Long id);
    /** Id do lançamento já criado com este cdRequisicao, se houver. */
    Optional<Long> buscarLancamentoPorRequisicao(String cdRequisicao);
    /** Insere e devolve o id; vazio se o cdRequisicao já existia (nada é gravado). */
    Optional<Long> inserirLancamento(FinanceiroLancamento l);
    /** Devolve quantas linhas foram alteradas (0 = versão diferente ou inexistente). */
    int alterarLancamento(FinanceiroLancamento l, int nrVersao);
    int marcarRealizado(Long id, boolean realizado, int nrVersao);
    int deletarLancamento(Long id, int nrVersao);

    /** Entradas, saídas e quantidades dos lançamentos filtrados. */
    Totais resumir(FinanceiroFiltro filtro);

    class Totais {
        public final BigDecimal entradas;
        public final BigDecimal saidas;
        public final BigDecimal entradasRealizadas;
        public final BigDecimal saidasRealizadas;
        public final int qtLancamentos;
        public final int qtRealizados;

        public Totais(BigDecimal entradas, BigDecimal saidas, BigDecimal entradasRealizadas,
                      BigDecimal saidasRealizadas, int qtLancamentos, int qtRealizados) {
            this.entradas = entradas;
            this.saidas = saidas;
            this.entradasRealizadas = entradasRealizadas;
            this.saidasRealizadas = saidasRealizadas;
            this.qtLancamentos = qtLancamentos;
            this.qtRealizados = qtRealizados;
        }
    }
}
