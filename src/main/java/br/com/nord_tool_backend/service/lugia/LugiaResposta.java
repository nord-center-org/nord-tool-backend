package br.com.nord_tool_backend.service.lugia;

/** Resultado de uma resposta de provedor/agente: texto completo, conteúdo nativo (JSON) e uso de tokens. */
public class LugiaResposta {

    private final String texto;
    private final String nativo;
    private final String motivo;
    private final Integer tokensEntrada;
    private final Integer tokensSaida;

    public LugiaResposta(String texto, String nativo, String motivo, Integer tokensEntrada, Integer tokensSaida) {
        this.texto = texto;
        this.nativo = nativo;
        this.motivo = motivo;
        this.tokensEntrada = tokensEntrada;
        this.tokensSaida = tokensSaida;
    }

    public String getTexto() { return texto; }
    public String getNativo() { return nativo; }
    public String getMotivo() { return motivo; }
    public Integer getTokensEntrada() { return tokensEntrada; }
    public Integer getTokensSaida() { return tokensSaida; }
}
