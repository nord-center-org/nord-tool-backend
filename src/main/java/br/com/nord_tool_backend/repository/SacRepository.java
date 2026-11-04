package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.dto.SacImagemDto;

import java.util.List;
import java.util.Optional;

public interface SacRepository {
    /** Todos os chamados (idUsuario null) ou só os de uma pessoa, do mais novo para o mais antigo. */
    List<SacChamadoDto> listar(Long idUsuario);

    Optional<SacChamadoDto> buscar(Long id);

    /** Imagens dos chamados listados (por chamado, na ordem). */
    List<SacImagemDto> imagens(List<Long> idsChamados);

    Optional<SacImagemDto> imagem(Long idImagem);

    Long inserir(Long idUsuario, String nmTitulo, String txDescricao, String txTela);

    void inserirImagem(Long idChamado, Long idArquivo, int nrOrdem);

    int responder(Long id, String cdStatus, String txResposta, Long idUsuarioResposta);

    /** Ids dos arquivos do chamado (para apagar depois de excluir). */
    List<Long> arquivos(Long idChamado);

    int excluir(Long id);
}
