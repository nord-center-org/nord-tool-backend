package br.com.nord_tool_backend.service.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instagram (API do Instagram com login do Instagram; conta profissional, de criador ou empresa). Publica Reels: o
 * Instagram busca o vídeo pelo endereço público e temporário que o NordTool gera, processa e publica.
 */
@Component
public class InstagramProvider extends ProviderHttp {

    static final String AUTH = "https://www.instagram.com/oauth/authorize";
    static final String TOKEN = "https://api.instagram.com/oauth/access_token";
    static final String GRAPH = "https://graph.instagram.com";
    static final String ESCOPOS = "instagram_business_basic,instagram_business_content_publish,instagram_business_manage_insights";
    static final int TENTATIVAS = 60;
    static final long PAUSA_MS = 5_000;

    private final String appId;
    private final String appSecret;
    private final String versao;

    @Autowired
    public InstagramProvider(RestTemplateBuilder builder,
                             @Value("${nord-tool.marketing.instagram.app-id:}") String appId,
                             @Value("${nord-tool.marketing.instagram.app-secret:}") String appSecret,
                             @Value("${nord-tool.marketing.instagram.versao:v24.0}") String versao) {
        this(builder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(30)).build(), appId, appSecret, versao);
    }

    public InstagramProvider(RestTemplate http, String appId, String appSecret, String versao) {
        super(http);
        this.appId = appId == null ? "" : appId.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
        this.versao = versao == null || versao.isBlank() ? "v24.0" : versao.trim();
    }

    @Override
    public RedeSocial rede() {
        return RedeSocial.INSTAGRAM;
    }

    @Override
    public boolean configurada() {
        return !appId.isEmpty() && !appSecret.isEmpty();
    }

    @Override
    public String urlAutorizacao(String redirectUri, String state) {
        return AUTH + "?client_id=" + enc(appId) + "&redirect_uri=" + enc(redirectUri) + "&response_type=code"
                + "&scope=" + enc(ESCOPOS) + "&state=" + enc(state);
    }

    private String api(String caminho) {
        return GRAPH + "/" + versao + caminho;
    }

    @Override
    public TokensRede conectar(String codigo, String redirectUri) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("client_id", appId);
        f.put("client_secret", appSecret);
        f.put("grant_type", "authorization_code");
        f.put("redirect_uri", redirectUri);
        f.put("code", codigo);
        JsonNode curto = postForm(TOKEN, f);
        if (curto.path("data").isArray() && curto.path("data").size() > 0) curto = curto.path("data").get(0);
        String tokenCurto = curto.path("access_token").asText("");
        if (tokenCurto.isEmpty()) throw new FalhaRedeException("Instagram não devolveu o acesso. Tente conectar de novo.");
        // Troca pelo token de 60 dias.
        JsonNode longo = get(GRAPH + "/access_token?grant_type=ig_exchange_token&client_secret=" + enc(appSecret)
                + "&access_token=" + enc(tokenCurto), null);
        String token = longo.path("access_token").asText(tokenCurto);
        JsonNode eu = get(api("/me?fields=user_id,username,name,profile_picture_url&access_token=" + enc(token)), null);
        String id = eu.path("user_id").asText(eu.path("id").asText(""));
        String nome = eu.path("username").asText(eu.path("name").asText("Instagram"));
        return new TokensRede(token, null, expira(longo, "expires_in"), id, "@" + nome, texto(eu.path("profile_picture_url")));
    }

    /** O token de 60 dias é renovado por mais 60 enquanto ainda vale. */
    @Override
    public TokensRede renovar(TokensRede atual) {
        JsonNode r = get(GRAPH + "/refresh_access_token?grant_type=ig_refresh_token&access_token=" + enc(atual.getTokenAcesso()), null);
        String token = r.path("access_token").asText("");
        if (token.isEmpty()) throw new FalhaRedeException("O acesso ao Instagram venceu. Conecte a conta de novo.");
        LocalDateTime vence = expira(r, "expires_in");
        return new TokensRede(token, null, vence, atual.getIdExterno(), atual.getNome(), atual.getFoto());
    }

    @Override
    public PainelRede painel(String token, String idExterno) {
        JsonNode eu = get(api("/me?fields=followers_count,media_count&access_token=" + enc(token)), null);
        JsonNode midias = get(api("/me/media?limit=12&fields=id,caption,media_type,media_product_type,permalink,thumbnail_url,media_url,"
                + "timestamp,like_count,comments_count&access_token=" + enc(token)), null).path("data");
        List<PostRede> posts = new ArrayList<>();
        long total = 0;
        boolean temViews = false;
        for (JsonNode m : midias) {
            String id = m.path("id").asText();
            Long views = views(id, token);
            if (views != null) {
                total += views;
                temViews = true;
            }
            String miniatura = texto(m.path("thumbnail_url"));
            if (miniatura == null && !"VIDEO".equals(m.path("media_type").asText())) miniatura = texto(m.path("media_url"));
            posts.add(new PostRede(id, legendaCurta(m.path("caption").asText("")), miniatura, texto(m.path("permalink")),
                    texto(m.path("timestamp")), views, numero(m.path("like_count")), numero(m.path("comments_count"))));
        }
        return new PainelRede(numero(eu.path("followers_count")), temViews ? total : null, posts);
    }

    /** Views do post (métrica "views"); nulo quando o Instagram não informa para esse tipo de post. */
    private Long views(String id, String token) {
        try {
            JsonNode d = get(api("/" + id + "/insights?metric=views&access_token=" + enc(token)), null).path("data");
            if (!d.isArray() || d.size() == 0) return null;
            JsonNode v = d.get(0).at("/values/0/value");
            if (v.isMissingNode()) v = d.get(0).at("/total_value/value");
            return numero(v);
        } catch (FalhaRedeException e) {
            return null;
        }
    }

    private static String legendaCurta(String legenda) {
        String l = legenda.replace('\n', ' ').trim();
        return l.length() > 80 ? l.substring(0, 80) + "…" : l;
    }

    @Override
    public ResultadoPublicacao publicar(String token, String idExterno, VideoParaPublicar video) {
        if (video.getUrlPublica() == null) throw new FalhaRedeException("Instagram: falta o endereço público do vídeo.");
        Map<String, String> f = new LinkedHashMap<>();
        f.put("media_type", "REELS");
        f.put("video_url", video.getUrlPublica());
        f.put("caption", legenda(video));
        f.put("share_to_feed", "true");
        f.put("access_token", token);
        String container = postForm(api("/" + idExterno + "/media"), f).path("id").asText("");
        if (container.isEmpty()) throw new FalhaRedeException("Instagram não aceitou o vídeo.");
        esperarProcessar(container, token);
        Map<String, String> p = new LinkedHashMap<>();
        p.put("creation_id", container);
        p.put("access_token", token);
        String id = postForm(api("/" + idExterno + "/media_publish"), p).path("id").asText("");
        if (id.isEmpty()) throw new FalhaRedeException("Instagram não confirmou a publicação.");
        String link = permalink(id, token);
        String aviso = video.isPublico() ? null : "O Instagram não tem post privado pela API: o Reel saiu público.";
        return new ResultadoPublicacao(id, link, true, aviso);
    }

    /** Link do post publicado; nulo se o Instagram não informar (o post já saiu). */
    private String permalink(String id, String token) {
        try {
            return texto(get(api("/" + id + "?fields=permalink&access_token=" + enc(token)), null).path("permalink"));
        } catch (FalhaRedeException e) {
            return null;
        }
    }

    private void esperarProcessar(String container, String token) {
        for (int i = 0; i < TENTATIVAS; i++) {
            JsonNode s = get(api("/" + container + "?fields=status_code,status&access_token=" + enc(token)), null);
            String codigo = s.path("status_code").asText("");
            if ("FINISHED".equals(codigo)) return;
            if ("ERROR".equals(codigo) || "EXPIRED".equals(codigo)) {
                throw new FalhaRedeException("Instagram não conseguiu processar o vídeo (" + corta(s.path("status").asText(codigo))
                        + "). Use MP4 (H.264), vertical 9:16, de 3 s a 15 min.");
            }
            esperar(PAUSA_MS);
        }
        throw new FalhaRedeException("Instagram demorou demais para processar o vídeo. Tente de novo mais tarde.");
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
