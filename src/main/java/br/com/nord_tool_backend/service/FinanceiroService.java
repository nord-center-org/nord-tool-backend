package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.dto.FinanceiroCategoriaDto;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroListaDto;
import br.com.nord_tool_backend.dto.FinanceiroPessoaDto;
import br.com.nord_tool_backend.dto.FinanceiroResumoDto;
import br.com.nord_tool_backend.form.FinanceiroCategoriaForm;
import br.com.nord_tool_backend.form.FinanceiroLancamentoForm;
import br.com.nord_tool_backend.form.FinanceiroPessoaForm;
import br.com.nord_tool_backend.form.FinanceiroRealizadoForm;

import java.util.List;

public interface FinanceiroService {

    FinanceiroListaDto listar(FinanceiroFiltro filtro);

    FinanceiroResumoDto resumir(FinanceiroFiltro filtro);

    /**
     * Cria o lançamento (ou N parcelas mensais). O mesmo cdRequisicao devolve o que já foi criado, sem duplicar.
     * {@code idUsuario} é quem digitou (login); pode ser nulo com a segurança desligada.
     */
    List<FinanceiroLancamentoDto> criar(FinanceiroLancamentoForm form, Long idUsuario);

    /** Edita; versão divergente → 409. Mês fechado → 400. {@code idUsuario} registra a leitura se for fatura. */
    FinanceiroLancamentoDto alterar(Long id, FinanceiroLancamentoForm form, Long idUsuario);

    /** Marca como recebido/pago ou volta para previsto; versão divergente → 409. */
    FinanceiroLancamentoDto marcarRealizado(Long id, FinanceiroRealizadoForm form);

    /** Exclui o lançamento. Versão divergente → 409. */
    void excluir(Long id, Integer nrVersao);

    List<FinanceiroPessoaDto> listarPessoas();

    FinanceiroPessoaDto criarPessoa(FinanceiroPessoaForm form);

    FinanceiroPessoaDto atualizarPessoa(Long id, FinanceiroPessoaForm form);

    List<FinanceiroCategoriaDto> listarCategorias();

    FinanceiroCategoriaDto criarCategoria(FinanceiroCategoriaForm form);

    FinanceiroCategoriaDto atualizarCategoria(Long id, FinanceiroCategoriaForm form);
}
