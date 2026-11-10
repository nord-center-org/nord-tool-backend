package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.form.CubicagemForm;

import java.util.List;

public interface CubicagemService {

    List<CubicagemDto> listar();

    CubicagemDto abrir(Long id);

    CubicagemDto criar(CubicagemForm form);

    CubicagemDto salvar(Long id, CubicagemForm form);

    void excluir(Long id);
}
