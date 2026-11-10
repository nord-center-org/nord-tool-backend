package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.form.BlocoArquivoForm;

import java.util.List;

/** Bloco de notas v3: planilhas e documentos avulsos. */
public interface BlocoArquivoService {

    List<BlocoArquivoDto> listar();

    BlocoArquivoDto abrir(Long id);

    BlocoArquivoDto criar(BlocoArquivoForm form);

    BlocoArquivoDto salvar(Long id, BlocoArquivoForm form);

    void excluir(Long id);
}
