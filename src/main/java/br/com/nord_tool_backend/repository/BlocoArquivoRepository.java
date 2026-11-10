package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.BlocoArquivoDto;

import java.util.List;
import java.util.Optional;

/** Planilhas e documentos avulsos do bloco de notas: toda leitura filtra pelo dono. */
public interface BlocoArquivoRepository {

    List<BlocoArquivoDto> listar(Long idUsuario);

    /** Com o conteúdo. */
    Optional<BlocoArquivoDto> buscar(Long id, Long idUsuario);

    int contar(Long idUsuario);

    Long inserir(Long idUsuario, String tipo, String titulo, String cor, String conteudo);

    /** Conteúdo nulo mantém o atual. Devolve 0 se a versão não bate. */
    int alterar(Long id, Long idUsuario, String titulo, String cor, String conteudo, int nrVersao);

    int excluir(Long id, Long idUsuario);
}
