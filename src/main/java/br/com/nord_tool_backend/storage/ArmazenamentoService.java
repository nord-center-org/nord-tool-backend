package br.com.nord_tool_backend.storage;

/**
 * Porta de armazenamento de arquivos. Telas e APIs dependem só desta interface, para que o
 * provedor possa ser trocado sem mexer nelas. Seleção por {@code nord-tool.storage.provider}.
 *
 * Provedores:
 *  - POSTGRES (padrão, provisório): {@link ArmazenamentoPostgresServiceImpl}.
 *  - TODO: provedor definitivo — decisão pendente (Nicolas + Nicolei). Entrará como
 *    ArmazenamentoDriveServiceImpl ou ArmazenamentoS3ServiceImpl, gravando a chave externa em
 *    arquivo_armazenado.cd_referencia e nm_provedor, sem alterar controllers nem telas.
 *
 * Quem recebe upload deve validar antes com {@link ArquivoValidador}.
 */
public interface ArmazenamentoService {

    /** Grava o arquivo e devolve o id em arquivo_armazenado. */
    Long salvar(String nome, String contentType, byte[] bytes);

    /** Lê o arquivo com seus bytes. Lança 404 se não existir. */
    ArquivoConteudo abrir(Long idArquivo);

    /** Apaga o arquivo. Idempotente: id inexistente não gera erro. */
    void apagar(Long idArquivo);
}
