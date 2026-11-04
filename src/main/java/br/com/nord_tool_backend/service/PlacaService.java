package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.form.PlacaForm;
import br.com.nord_tool_backend.form.PlacaModeloForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface PlacaService {
    List<PlacaModeloDto> modelos();

    PlacaModeloDto criarModelo(PlacaModeloForm form);

    PlacaModeloDto alterarModelo(Long id, PlacaModeloForm form);

    void excluirModelo(Long id);

    /** tipo: fundo ou logo. */
    PlacaModeloDto salvarImagem(Long idModelo, String tipo, String nome, byte[] bytes);

    PlacaModeloDto removerImagem(Long idModelo, String tipo);

    ArquivoDownload baixarImagem(Long idModelo, String tipo);

    List<PlacaDto> placas();

    PlacaDto criar(PlacaForm form);

    PlacaDto alterar(Long id, PlacaForm form);

    void excluir(Long id);
}
