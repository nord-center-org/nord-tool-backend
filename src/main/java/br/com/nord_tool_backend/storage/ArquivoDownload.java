package br.com.nord_tool_backend.storage;

/** Arquivo pronto para resposta HTTP, com a versão (epoch millis) usada em ETag/cache. */
public class ArquivoDownload {
    private final ArquivoConteudo conteudo;
    private final long versao;

    public ArquivoDownload(ArquivoConteudo conteudo, long versao) {
        this.conteudo = conteudo;
        this.versao = versao;
    }

    public ArquivoConteudo getConteudo() { return conteudo; }
    public long getVersao() { return versao; }
}
