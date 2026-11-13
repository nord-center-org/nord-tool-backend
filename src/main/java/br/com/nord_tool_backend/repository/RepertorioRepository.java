package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;

import java.util.List;
import java.util.Optional;

public interface RepertorioRepository {

    List<RepertorioMusicaDto> listarMusicas(Long idUsuario);

    Optional<RepertorioMusicaDto> buscarMusica(Long idUsuario, String chave);

    int contarMusicas(Long idUsuario);

    /** Soma dos bytes de áudio guardados pela pessoa, sem contar a música `excetoChave` (null = todas). */
    long bytesUsados(Long idUsuario, String excetoChave);

    /** Insere ou atualiza (pela chave) os dados da música. */
    void salvarMusica(Long idUsuario, String chave, RepertorioMusicaDto dados);

    void ligarAudio(Long idUsuario, String chave, Long idArquivo, String nomeArquivo, long tamanho);

    int excluirMusica(Long idUsuario, String chave);

    List<RepertorioProjetoDto> listarProjetos(Long idUsuario);

    Optional<RepertorioProjetoDto> buscarProjeto(Long idUsuario, String chave);

    int contarProjetos(Long idUsuario);

    void salvarProjeto(Long idUsuario, String chave, String titulo, String dados);

    int excluirProjeto(Long idUsuario, String chave);
}
