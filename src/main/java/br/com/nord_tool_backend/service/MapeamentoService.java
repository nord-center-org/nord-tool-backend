package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.form.MapeamentoForm;
import br.com.nord_tool_backend.form.MapeamentoStatusForm;

import java.util.List;

public interface MapeamentoService {

    List<MapeamentoDto> listar();

    MapeamentoDto detalhar(Long id);

    MapeamentoDto criar(MapeamentoForm form);

    MapeamentoDto alterar(Long id, MapeamentoForm form);

    MapeamentoDto alterarStatus(Long id, MapeamentoStatusForm form);

    void excluir(Long id, Integer nrVersao);
}
