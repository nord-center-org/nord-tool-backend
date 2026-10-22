package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.form.RelatorioFotoForm;
import br.com.nord_tool_backend.form.RelatorioFotograficoForm;
import br.com.nord_tool_backend.form.RelatorioModeloForm;
import br.com.nord_tool_backend.form.RelatorioStatusForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface RelatorioFotograficoService {

    List<RelatorioModeloDto> listarModelos();

    RelatorioModeloDto criarModelo(RelatorioModeloForm form);

    RelatorioModeloDto alterarModelo(Long id, RelatorioModeloForm form);

    List<RelatorioFotograficoDto> listar();

    RelatorioFotograficoDto detalhar(Long id);

    RelatorioFotograficoDto criar(RelatorioFotograficoForm form);

    RelatorioFotograficoDto alterar(Long id, RelatorioFotograficoForm form);

    RelatorioFotograficoDto alterarStatus(Long id, RelatorioStatusForm form);

    void excluir(Long id, Integer nrVersao);

    RelatorioFotoDto adicionarFoto(Long idItem, String nome, byte[] bytes, String txLegenda);

    RelatorioFotoDto alterarFoto(Long idFoto, RelatorioFotoForm form);

    void excluirFoto(Long idFoto);

    ArquivoDownload baixarFoto(Long idFoto);
}
