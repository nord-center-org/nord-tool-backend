package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.MedicaoDto;
import br.com.nord_tool_backend.dto.MedicaoListaDto;
import br.com.nord_tool_backend.form.MedicaoForm;
import br.com.nord_tool_backend.form.MedicaoStatusForm;

public interface MedicaoService {
    MedicaoListaDto listar();
    MedicaoDto criar(MedicaoForm form);
    MedicaoDto alterar(Long id, MedicaoForm form);
    MedicaoDto alterarStatus(Long id, MedicaoStatusForm form);
    void excluir(Long id, Integer nrVersao);
}
