package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.FinanceiroAtivoDto;
import br.com.nord_tool_backend.dto.FinanceiroInvestimentoDto;
import br.com.nord_tool_backend.form.FinanceiroAtivoForm;
import br.com.nord_tool_backend.form.FinanceiroOperacaoForm;
import br.com.nord_tool_backend.form.FinanceiroProventoForm;

public interface FinanceiroInvestimentoService {

    /** Fundos (de uma pessoa ou de todas) com cotação ao vivo, resultado e proventos a receber. */
    FinanceiroInvestimentoDto listar(Long idPessoa);

    FinanceiroAtivoDto criarAtivo(FinanceiroAtivoForm form);

    FinanceiroAtivoDto atualizarAtivo(Long id, FinanceiroAtivoForm form);

    /** Idempotente por cdRequisicao. Venda acima das cotas que se tinha é recusada. */
    FinanceiroAtivoDto registrarOperacao(Long idAtivo, FinanceiroOperacaoForm form);

    FinanceiroAtivoDto excluirOperacao(Long idOperacao);

    FinanceiroAtivoDto registrarProvento(Long idAtivo, FinanceiroProventoForm form);

    FinanceiroAtivoDto excluirProvento(Long idProvento);

    /** Importa os proventos do provedor (sem sobrescrever os digitados). Devolve quantos foram gravados. */
    int sincronizarProventos(Long idAtivo);
}
