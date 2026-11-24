package br.com.nord_tool_backend.service.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube (Google OAuth + YouTube Data API v3). Envio "resumable" em uma única parte, direto do disco. Enquanto o
 * projeto do Google não passa pela auditoria, o YouTube deixa os vídeos enviados pela API como privados.
 */
@Component
public class YoutubeProvider extends ProviderHttp {

    static final String AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN = "https://oauth2.googleapis.com/token";
    static final String API = "https://www.googleapis.com/youtube/v3";
    static final String UPLOAD = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
    static final String ESCOPOS = "https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly";

    private final String clientId;
    private final String clientSecret;

    @Autowired
    public YoutubeProvider(RestTemplateBuilder builder,
                           @Value("${nord-tool.marketing.youtube.client-id:}") String clientId,
                           @Value("${nord-tool.marketing.youtube.client-secret:}") String clientSecret) {
        this(builder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(30)).build(), clientId, clientSecret);
    }

    public YoutubeProvider(RestTemplate http, String clientId, String clientSecret) {
        super(http);
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    @Override
    public RedeSocial rede() {
        return RedeSocial.YOUTUBE;
    }

    @Override
    public boolean configurada() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    @Override
    public String urlAutorizacao(String redirectUri, String state) {
        return AUTH + "?client_id=" + enc(clientId) + "&redirect_uri=" + enc(redirectUri) + "&response_type=code"
                + "&scope=" + enc(ESCOPOS) + "&access_type=offline&prompt=consent&include_granted_scopes=true&state=" + enc(state);
    }

    @Override
    public TokensRede conectar(String codigo, String redirectUri) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("code", codigo);
        f.put("client_id", clientId);
        f.put("client_secret", clientSecret);
        f.put("redirect_uri", redirectUri);
        f.put("grant_type", "authorization_code");
        JsonNode t = postForm(TOKEN, f);
        String acesso = t.path("access_token").asText("");
        if (acesso.isEmpty()) throw new FalhaRedeException("YouTube não devolveu o acesso. Tente conectar de novo.");
        JsonNode canal = canal(acesso);
        return new TokensRede(acesso, texto(t.path("refresh_token")), expira(t, "expires_in"), canal.path("id").asText(),
                canal.at("/snippet/title").asText("Canal do YouTube"), texto(canal.at("/snippet/thumbnails/default/url")));
    }

    private JsonNode canal(String acesso) {
        JsonNode itens = get(API + "/channels?part=snippet,statistics,contentDetails&mine=true", acesso).path("items");
        if (!itens.isArray() || itens.size() == 0) {
            throw new FalhaRedeException("Esta conta Google não tem canal no YouTube. Crie o canal e conecte de novo.");
        }
        return itens.get(0);
    }

    @Override
    public TokensRede renovar(TokensRede atual) {
        if (atual.getTokenRenovacao() == null) throw new FalhaRedeException("O acesso ao YouTube venceu. Conecte a conta de novo.");
        Map<String, String> f = new LinkedHashMap<>();
        f.put("client_id", clientId);
        f.put("client_secret", clientSecret);
        f.put("refresh_token", atual.getTokenRenovacao());
        f.put("grant_type", "refresh_token");
        JsonNode t = postForm(TOKEN, f);
        String novo = t.path("refresh_token").asText(atual.getTokenRenovacao());
        return new TokensRede(t.path("access_token").asText(), novo, expira(t, "expires_in"), atual.getIdExterno(), atual.getNome(), atual.getFoto());
    }

    @Override
    public PainelRede painel(String acesso, String idExterno) {
        JsonNode canal = canal(acesso);
        String uploads = canal.at("/contentDetails/relatedPlaylists/uploads").asText("");
        List<PostRede> posts = new ArrayList<>();
        if (!uploads.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (JsonNode i : get(API + "/playlistItems?part=contentDetails&maxResults=12&playlistId=" + enc(uploads), acesso).path("items")) {
                String id = i.at("/contentDetails/videoId").asText("");
                if (!id.isEmpty()) ids.add(id);
            }
            if (!ids.isEmpty()) {
                for (JsonNode v : get(API + "/videos?part=snippet,statistics&id=" + enc(String.join(",", ids)), acesso).path("items")) {
                    String id = v.path("id").asText();
                    posts.add(new PostRede(id, v.at("/snippet/title").asText(""), texto(v.at("/snippet/thumbnails/medium/url")),
                            "https://youtu.be/" + id, texto(v.at("/snippet/publishedAt")), numero(v.at("/statistics/viewCount")),
                            numero(v.at("/statistics/likeCount")), numero(v.at("/statistics/commentCount"))));
                }
            }
        }
        return new PainelRede(numero(canal.at("/statistics/subscriberCount")), numero(canal.at("/statistics/viewCount")), posts);
    }

    @Override
    public ResultadoPublicacao publicar(String acesso, String idExterno, VideoParaPublicar video) {
        ObjectNode meta = json.createObjectNode();
        ObjectNode snippet = meta.putObject("snippet");
        snippet.put("title", corta100(video.getTitulo()));
        snippet.put("description", video.getLegenda() == null ? "" : video.getLegenda());
        snippet.put("categoryId", "22");
        ObjectNode status = meta.putObject("status");
        status.put("privacyStatus", video.isPublico() ? "public" : "private");
        status.put("selfDeclaredMadeForKids", false);

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(acesso);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Upload-Content-Length", String.valueOf(video.getTamanho()));
        h.set("X-Upload-Content-Type", video.getTipo());
        String sessao;
        try {
            ResponseEntity<String> r = http.exchange(URI.create(UPLOAD), HttpMethod.POST, new HttpEntity<>(meta.toString(), h), String.class);
            sessao = r.getHeaders().getFirst(HttpHeaders.LOCATION);
        } catch (RestClientResponseException e) {
            throw new FalhaRedeException("YouTube recusou: " + motivo(e.getResponseBodyAsString(), e.getRawStatusCode()));
        } catch (RestClientException e) {
            throw new FalhaRedeException("YouTube não respondeu. Tente de novo em instantes.");
        }
        if (sessao == null) throw new FalhaRedeException("YouTube não abriu o envio do vídeo.");

        Map<String, String> cab = new LinkedHashMap<>();
        cab.put("Content-Type", video.getTipo());
        RespostaEnvio envio = enviarTrecho(sessao, "PUT", cab, video.getArquivo(), 0, video.getTamanho());
        if (envio.status < 200 || envio.status >= 300) {
            throw new FalhaRedeException("YouTube recusou o vídeo: " + motivo(envio.corpo, envio.status));
        }
        JsonNode v = ler(envio.corpo);
        String id = v.path("id").asText("");
        if (id.isEmpty()) throw new FalhaRedeException("YouTube não confirmou o vídeo.");
        boolean publico = "public".equals(v.at("/status/privacyStatus").asText("public"));
        String aviso = video.isPublico() && !publico
                ? "O YouTube deixou o vídeo privado: enquanto o projeto do Google não passa pela auditoria da API, os envios ficam privados. Dá para torná-lo público no YouTube Studio."
                : null;
        return new ResultadoPublicacao(id, "https://youtu.be/" + id, publico, aviso);
    }

    private static String corta100(String s) {
        String t = s == null ? "" : s.replace('<', ' ').replace('>', ' ').trim();
        return t.length() > 100 ? t.substring(0, 100) : t;
    }
}
