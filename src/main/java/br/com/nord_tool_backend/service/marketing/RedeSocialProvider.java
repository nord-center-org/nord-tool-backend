package br.com.nord_tool_backend.service.marketing;

/**
 * Integração com uma rede (YouTube, Instagram, TikTok): login oficial da rede (OAuth), publicar e ler as métricas.
 * As chaves do app de cada rede vêm do ambiente e nunca vão para o cliente nem para o log.
 */
public interface RedeSocialProvider {

    RedeSocial rede();

    /** As chaves do app da rede estão no ambiente. */
    boolean configurada();

    /** Endereço da tela de login da rede (a pessoa autoriza e volta para {@code redirectUri} com o código). */
    String urlAutorizacao(String redirectUri, String state);

    /** Troca o código da volta do login pelos tokens e lê quem é a conta. */
    TokensRede conectar(String codigo, String redirectUri);

    /** Renova o token de acesso. */
    TokensRede renovar(TokensRede atual);

    PainelRede painel(String tokenAcesso, String idExterno);

    ResultadoPublicacao publicar(String tokenAcesso, String idExterno, VideoParaPublicar video);
}
