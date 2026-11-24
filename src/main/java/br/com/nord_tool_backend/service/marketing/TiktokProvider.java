package br.com.nord_tool_backend.service.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TikTok (Login Kit + Content Posting API, postagem direta). Enquanto o app do TikTok não passa pela auditoria, o
 * TikTok só aceita posts privados ("só eu"): o NordTool tenta público quando pedido e, se recusado, posta privado e avisa.
 */
@Component
public class TiktokProvider extends ProviderHttp {

    static final String AUTH = "https://www.tiktok.com/v2/auth/authorize/";
    static final String API = "https://open.tiktokapis.com/v2";
    static final String ESCOPOS = "user.info.basic,user.info.stats,video.list,video.publish";
    static final long PARTE = 10L * 1024 * 1024;
    static final int TENTATIVAS = 60;
    static final long PAUSA_MS = 5_000;
    private static final String PUBLICO = "PUBLIC_TO_EVERYONE";
    private static final String PRIVADO = "SELF_ONLY";

    private final String clientKey;
    private final String clientSecret;

    @Autowired
    public TiktokProvider(RestTemplateBuilder builder,
                          @Value("${nord-tool.marketing.tiktok.client-key:}") String clientKey,
                          @Value("${nord-tool.marketing.tiktok.client-secret:}") String clientSecret) {
        this(builder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(30)).build(), clientKey, clientSecret);
    }

    public TiktokProvider(RestTemplate http, String clientKey, String clientSecret) {
        super(http);
        this.clientKey = clientKey == null ? "" : clientKey.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    @Override
    public RedeSocial rede() {
        return RedeSocial.TIKTOK;
    }

    @Override
    public boolean configurada() {
        return !clientKey.isEmpty() && !clientSecret.isEmpty();
    }

    @Override
    public String urlAutorizacao(String redirectUri, String state) {
        return AUTH + "?client_key=" + enc(clientKey) + "&scope=" + enc(ESCOPOS) + "&response_type=code&redirect_uri="
                + enc(redirectUri) + "&state=" + enc(state);
    }

    @Override
    public TokensRede conectar(String codigo, String redirectUri) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("client_key", clientKey);
        f.put("client_secret", clientSecret);
        f.put("code", codigo);
        f.put("grant_type", "authorization_code");
        f.put("redirect_uri", redirectUri);
        return tokens(postForm(API + "/oauth/token/", f), null);
    }

    @Override
    public TokensRede renovar(TokensRede atual) {
        if (atual.getTokenRenovacao() == null) throw new FalhaRedeException("O acesso ao TikTok venceu. Conecte a conta de novo.");
        Map<String, String> f = new LinkedHashMap<>();
        f.put("client_key", clientKey);
        f.put("client_secret", clientSecret);
        f.put("grant_type", "refresh_token");
        f.put("refresh_token", atual.getTokenRenovacao());
        return tokens(postForm(API + "/oauth/token/", f), atual);
    }

    private TokensRede tokens(JsonNode t, TokensRede atual) {
        String acesso = t.path("access_token").asText("");
        if (acesso.isEmpty()) {
            throw new FalhaRedeException("TikTok recusou: " + t.path("error_description").asText(t.path("error").asText("sem acesso")));
        }
        String renovacao = t.path("refresh_token").asText(atual == null ? null : atual.getTokenRenovacao());
        if (atual != null) {
            return new TokensRede(acesso, renovacao, expira(t, "expires_in"), atual.getIdExterno(), atual.getNome(), atual.getFoto());
        }
        JsonNode u = usuario(acesso);
        return new TokensRede(acesso, renovacao, expira(t, "expires_in"), t.path("open_id").asText(u.path("open_id").asText("")),
                u.path("display_name").asText("TikTok"), texto(u.path("avatar_url")));
    }

    private JsonNode usuario(String acesso) {
        JsonNode r = get(API + "/user/info/?fields=open_id,display_name,avatar_url,follower_count,likes_count,video_count", acesso);
        verificar(r);
        return r.at("/data/user");
    }

    /** O TikTok responde 200 com {@code error.code} diferente de "ok" em alguns erros. */
    private void verificar(JsonNode r) {
        String codigo = r.at("/error/code").asText("ok");
        if (!"ok".equals(codigo)) throw new FalhaRedeException("TikTok recusou: " + corta(r.at("/error/message").asText(codigo)));
    }

    @Override
    public PainelRede painel(String acesso, String idExterno) {
        JsonNode u = usuario(acesso);
        ObjectNode corpo = json.createObjectNode().put("max_count", 12);
        JsonNode r = postJson(API + "/video/list/?fields=id,title,video_description,cover_image_url,share_url,view_count,like_count,"
                + "comment_count,create_time", acesso, corpo.toString());
        verificar(r);
        List<PostRede> posts = new ArrayList<>();
        long total = 0;
        for (JsonNode v : r.at("/data/videos")) {
            Long views = numero(v.path("view_count"));
            if (views != null) total += views;
            String titulo = v.path("title").asText(v.path("video_description").asText(""));
            long criado = v.path("create_time").asLong(0);
            posts.add(new PostRede(v.path("id").asText(), titulo, texto(v.path("cover_image_url")), texto(v.path("share_url")),
                    criado > 0 ? java.time.Instant.ofEpochSecond(criado).toString() : null, views, numero(v.path("like_count")),
                    numero(v.path("comment_count"))));
        }
        return new PainelRede(numero(u.path("follower_count")), posts.isEmpty() ? null : total, posts);
    }

    @Override
    public ResultadoPublicacao publicar(String acesso, String idExterno, VideoParaPublicar video) {
        JsonNode info = postJson(API + "/post/publish/creator_info/query/", acesso, "{}");
        verificar(info);
        List<String> opcoes = new ArrayList<>();
        info.at("/data/privacy_level_options").forEach(o -> opcoes.add(o.asText()));
        boolean querPublico = video.isPublico() && opcoes.contains(PUBLICO);
        String privacidade;
        if (querPublico) privacidade = PUBLICO;
        else if (opcoes.isEmpty() || opcoes.contains(PRIVADO)) privacidade = PRIVADO;
        else privacidade = opcoes.get(0);
        String aviso = video.isPublico() && !querPublico
                ? "O TikTok só liberou post privado para esta conta. Dá para torná-lo público no app do TikTok." : null;

        JsonNode inicio;
        try {
            inicio = iniciar(acesso, video, privacidade);
        } catch (FalhaRedeException e) {
            if (!PUBLICO.equals(privacidade)) throw e;
            // App ainda sem auditoria: o TikTok só aceita privado.
            privacidade = PRIVADO;
            aviso = "O TikTok postou como privado (\"só eu\"): enquanto o app do NordTool não passa pela auditoria do TikTok, "
                    + "os posts pela API ficam privados. Dá para torná-lo público no app do TikTok.";
            inicio = iniciar(acesso, video, privacidade);
        }
        String publishId = inicio.at("/data/publish_id").asText("");
        String uploadUrl = inicio.at("/data/upload_url").asText("");
        if (publishId.isEmpty() || uploadUrl.isEmpty()) throw new FalhaRedeException("TikTok não abriu o envio do vídeo.");
        enviarPartes(uploadUrl, video);
        String id = aguardar(acesso, publishId);
        return new ResultadoPublicacao(id == null ? publishId : id, null, PUBLICO.equals(privacidade), aviso);
    }

    private JsonNode iniciar(String acesso, VideoParaPublicar video, String privacidade) {
        long[] partes = partes(video.getTamanho());
        ObjectNode corpo = json.createObjectNode();
        ObjectNode post = corpo.putObject("post_info");
        post.put("title", legenda(video));
        post.put("privacy_level", privacidade);
        post.put("disable_duet", false);
        post.put("disable_comment", false);
        post.put("disable_stitch", false);
        ObjectNode fonte = corpo.putObject("source_info");
        fonte.put("source", "FILE_UPLOAD");
        fonte.put("video_size", video.getTamanho());
        fonte.put("chunk_size", partes[0]);
        fonte.put("total_chunk_count", partes[1]);
        JsonNode r = postJson(API + "/post/publish/video/init/", acesso, corpo.toString());
        verificar(r);
        return r;
    }

    /** Tamanho da parte e quantas partes: até 10 MB vai inteiro; acima, partes de 10 MB e a sobra entra na última. */
    static long[] partes(long tamanho) {
        if (tamanho < PARTE) return new long[]{tamanho, 1};
        long total = Math.max(1, tamanho / PARTE);
        return new long[]{PARTE, total};
    }

    private void enviarPartes(String url, VideoParaPublicar video) {
        long[] p = partes(video.getTamanho());
        long tamanho = video.getTamanho();
        for (long i = 0; i < p[1]; i++) {
            long inicio = i * p[0];
            long fim = i == p[1] - 1 ? tamanho - 1 : inicio + p[0] - 1;
            Map<String, String> cab = new LinkedHashMap<>();
            cab.put("Content-Type", video.getTipo());
            cab.put("Content-Range", "bytes " + inicio + "-" + fim + "/" + tamanho);
            RespostaEnvio r = enviarTrecho(url, "PUT", cab, video.getArquivo(), inicio, fim - inicio + 1);
            if (r.status != 201 && r.status != 206 && (r.status < 200 || r.status >= 300)) {
                throw new FalhaRedeException("TikTok recusou o vídeo: " + motivo(r.corpo, r.status));
            }
        }
    }

    /** Espera o TikTok publicar; devolve o id do post público, ou nulo se ficou privado/processando. */
    private String aguardar(String acesso, String publishId) {
        ObjectNode corpo = json.createObjectNode().put("publish_id", publishId);
        for (int i = 0; i < TENTATIVAS; i++) {
            JsonNode r = postJson(API + "/post/publish/status/fetch/", acesso, corpo.toString());
            verificar(r);
            String status = r.at("/data/status").asText("");
            if ("PUBLISH_COMPLETE".equals(status)) {
                JsonNode ids = r.at("/data/publicaly_available_post_id");
                return ids.isArray() && ids.size() > 0 ? ids.get(0).asText() : null;
            }
            if ("FAILED".equals(status)) {
                throw new FalhaRedeException("TikTok não publicou: " + corta(r.at("/data/fail_reason").asText("falhou")));
            }
            esperar(PAUSA_MS);
        }
        return null;
    }

    private static String legenda(VideoParaPublicar v) {
        String titulo = v.getTitulo() == null ? "" : v.getTitulo().trim();
        String legenda = v.getLegenda() == null ? "" : v.getLegenda().trim();
        String tudo;
        if (legenda.isEmpty()) tudo = titulo;
        else if (titulo.isEmpty()) tudo = legenda;
        else tudo = titulo + "\n\n" + legenda;
        return tudo.length() > 2200 ? tudo.substring(0, 2200) : tudo;
    }
}
