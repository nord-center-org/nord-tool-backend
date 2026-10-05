package br.com.nord_tool_backend.storage;

public class ArquivoConteudo {
    private final String nome;
    private final String contentType;
    private final long tamanho;
    private final byte[] bytes;

    public ArquivoConteudo(String nome, String contentType, long tamanho, byte[] bytes) {
        this.nome = nome;
        this.contentType = contentType;
        this.tamanho = tamanho;
        this.bytes = bytes;
    }

    public String getNome() { return nome; }
    public String getContentType() { return contentType; }
    public long getTamanho() { return tamanho; }
    public byte[] getBytes() { return bytes; }
}
