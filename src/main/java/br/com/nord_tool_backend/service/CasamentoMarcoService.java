package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.form.CasamentoMarcoForm;

import java.util.List;

public interface CasamentoMarcoService {
    List<CasamentoMarcoDto> listar();
    CasamentoMarcoDto buscar(Long id);
    CasamentoMarcoDto criar(CasamentoMarcoForm form);
    CasamentoMarcoDto alterar(Long id, CasamentoMarcoForm form);
    CasamentoMarcoDto concluir(Long id, boolean concluido);
    void deletar(Long id);

    /** Semeia os 10 marcos padrão; só funciona com a tabela vazia. */
    List<CasamentoMarcoDto> criarPadrao();
}
