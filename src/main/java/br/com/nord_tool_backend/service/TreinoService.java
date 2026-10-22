package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.form.TreinoForm;
import br.com.nord_tool_backend.form.TreinoSessaoForm;

import java.util.List;

public interface TreinoService {

    List<TreinoDto> listar();

    TreinoDto criar(TreinoForm form);

    TreinoDto alterar(Long id, TreinoForm form);

    void excluir(Long id, Integer nrVersao);

    List<TreinoSessaoDto> listarSessoes(Integer limite);

    TreinoSessaoDto registrarSessao(TreinoSessaoForm form);

    void excluirSessao(Long id);
}
