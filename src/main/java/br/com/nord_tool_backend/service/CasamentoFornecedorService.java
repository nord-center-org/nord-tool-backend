package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.form.CasamentoFornecedorForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface CasamentoFornecedorService {
    List<CasamentoFornecedorDto> listar();
    CasamentoFornecedorDto buscar(Long id);
    CasamentoFornecedorDto criar(CasamentoFornecedorForm form);
    CasamentoFornecedorDto alterar(Long id, CasamentoFornecedorForm form);
    /** Exclui o fornecedor, seus anexos e os arquivos armazenados. */
    void deletar(Long id);

    List<CasamentoAnexoDto> listarAnexos(Long idFornecedor);
    /** Contrato/comprovante: PDF ou imagem (JPEG/PNG) de até 15 MB. */
    CasamentoAnexoDto anexar(Long idFornecedor, String nomeArquivo, byte[] bytes, String descricao);
    ArquivoDownload baixarAnexo(Long idAnexo);
    void excluirAnexo(Long idAnexo);
}
