package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.dto.RepertorioUsoDto;
import br.com.nord_tool_backend.form.RepertorioMusicaForm;
import br.com.nord_tool_backend.form.RepertorioProjetoForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

/** Repertório v7: as músicas e as criações do estúdio guardadas na conta de quem está logado. */
public interface RepertorioService {

    List<RepertorioMusicaDto> listarMusicas();

    RepertorioMusicaDto abrirMusica(String chave);

    RepertorioMusicaDto salvarMusica(String chave, RepertorioMusicaForm form);

    RepertorioMusicaDto enviarAudio(String chave, String nome, byte[] bytes);

    ArquivoDownload baixarAudio(String chave);

    void excluirMusica(String chave);

    RepertorioUsoDto uso();

    List<RepertorioProjetoDto> listarProjetos();

    RepertorioProjetoDto abrirProjeto(String chave);

    RepertorioProjetoDto salvarProjeto(String chave, RepertorioProjetoForm form);

    void excluirProjeto(String chave);
}
