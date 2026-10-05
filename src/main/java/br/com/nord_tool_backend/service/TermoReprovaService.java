package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.TermoFotoDto;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoDto;
import br.com.nord_tool_backend.form.OrdemFotoForm;
import br.com.nord_tool_backend.form.SituacaoTermoForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface TermoReprovaService {

    List<TermoReprovaResumoDto> listarPorApartamento(Long idApartamento);

    TermoReprovaDto buscar(Long idTermo);

    /** Cria um novo termo com nr_termo = max + 1. */
    TermoReprovaDto criar(Long idApartamento, String nomeArquivo, byte[] pdf, int nrPaginas);

    /** Troca o PDF; remove (e avisa) as fotos de páginas que deixaram de existir. */
    TermoReprovaDto trocarArquivo(Long idTermo, String nomeArquivo, byte[] pdf, int nrPaginas);

    TermoReprovaDto atualizarSituacao(Long idTermo, SituacaoTermoForm form);

    /** Remove o termo, as fotos e todos os arquivos armazenados. */
    void deletar(Long idTermo);

    /** Chamado antes de excluir um apartamento: o ON DELETE CASCADE não apaga os arquivos. */
    void apagarPorApartamento(Long idApartamento);

    ArquivoDownload abrirPdf(Long idTermo);

    TermoFotoDto adicionarFoto(Long idTermo, String nomeImagem, byte[] imagem, byte[] miniatura, int nrPagina, String legenda);

    /** Campos nulos não são alterados; imagem e miniatura devem vir juntas. */
    TermoFotoDto editarFoto(Long idFoto, String nomeImagem, byte[] imagem, byte[] miniatura, String legenda, Integer nrPagina);

    void excluirFoto(Long idFoto);

    List<TermoFotoDto> ordenarFotos(Long idTermo, List<OrdemFotoForm> ordem);

    ArquivoDownload abrirImagem(Long idFoto);

    ArquivoDownload abrirMiniatura(Long idFoto);
}
