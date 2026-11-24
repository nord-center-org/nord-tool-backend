package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CasamentoArteDto;
import br.com.nord_tool_backend.form.CasamentoArteForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

/** Artes do casamento (convite, save the date, cardápio…): guardar, ver, renomear e excluir. */
public interface CasamentoArteService {

    List<CasamentoArteDto> listar();

    CasamentoArteDto enviar(String nomeArquivo, byte[] bytes, String titulo, String categoria);

    CasamentoArteDto atualizar(Long id, CasamentoArteForm form);

    ArquivoDownload baixar(Long id);

    void excluir(Long id);
}
