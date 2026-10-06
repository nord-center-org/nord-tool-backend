package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.dto.CaixinhaComprovanteDto;
import br.com.nord_tool_backend.dto.CaixinhaLancamentoDto;
import br.com.nord_tool_backend.dto.CaixinhaListaDto;
import br.com.nord_tool_backend.dto.CaixinhaResponsavelDto;
import br.com.nord_tool_backend.dto.CaixinhaResumoDto;
import br.com.nord_tool_backend.form.CaixinhaLancamentoForm;
import br.com.nord_tool_backend.form.CaixinhaMarcacaoForm;
import br.com.nord_tool_backend.form.CaixinhaResponsavelForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface CaixinhaService {

    CaixinhaListaDto listar(CaixinhaFiltro filtro);

    CaixinhaResumoDto resumir(CaixinhaFiltro filtro);

    /** Cria o lançamento; o mesmo cdRequisicao devolve o já criado (sem duplicar). */
    CaixinhaLancamentoDto criar(CaixinhaLancamentoForm form);

    /** Edita; versão divergente → 409. */
    CaixinhaLancamentoDto alterar(Long id, CaixinhaLancamentoForm form);

    CaixinhaLancamentoDto marcar(Long id, CaixinhaMarcacaoForm form);

    /** Exclui o lançamento e apaga os PDFs dos comprovantes. Versão divergente → 409. */
    void excluir(Long id, Integer nrVersao);

    List<CaixinhaResponsavelDto> listarResponsaveis();

    CaixinhaResponsavelDto criarResponsavel(CaixinhaResponsavelForm form);

    CaixinhaResponsavelDto atualizarResponsavel(Long id, CaixinhaResponsavelForm form);

    List<CaixinhaComprovanteDto> listarComprovantes(Long idLancamento);

    /** Anexa um PDF; o mesmo cdRequisicao devolve o comprovante já criado (envio idempotente). */
    CaixinhaComprovanteDto anexarComprovante(Long idLancamento, String nomeArquivo, byte[] bytes, String cdRequisicao);

    ArquivoDownload baixarComprovante(Long idComprovante);

    void excluirComprovante(Long idComprovante);
}
