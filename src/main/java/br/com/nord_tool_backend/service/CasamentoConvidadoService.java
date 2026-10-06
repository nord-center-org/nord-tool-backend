package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CasamentoConvidadoDto;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;

import java.util.List;

public interface CasamentoConvidadoService {
    List<CasamentoConvidadoDto> listar();
    CasamentoConvidadoDto buscar(Long id);
    CasamentoConvidadoDto criar(CasamentoConvidadoForm form);
    CasamentoConvidadoDto alterar(Long id, CasamentoConvidadoForm form);
    void deletar(Long id);

    /** Importa convidados de uma planilha .xlsx; linhas inválidas não impedem as demais. */
    ImportacaoConvidadosDto importar(String nomeArquivo, byte[] bytes);
}
