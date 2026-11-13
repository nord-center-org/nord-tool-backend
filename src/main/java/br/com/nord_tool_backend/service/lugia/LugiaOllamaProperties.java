package br.com.nord_tool_backend.service.lugia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Secretária local ({@code nord-tool.lugia.ollama.*}): o Ollama na máquina do dono do NordTool, alcançado por um túnel
 * reverso (Cloudflare Tunnel) fechado pelo Cloudflare Access. Fora de localhost, só liga com HTTPS e com um meio de
 * acesso (service token do Access ou Bearer); os segredos vêm só do ambiente e nunca vão para log ou resposta.
 */
@Component
public class LugiaOllamaProperties {

    private final String url;
    private final List<String> modelos;
    private final String cfAccessId;
    private final String cfAccessSecret;
    private final String token;
    private final int maxSimultaneos;
    private final int filaMax;
    private final long esperaMaxMs;
    private final long primeiroPedacoMs;
    private final long totalMs;
    private final int numCtx;
    private final int maxTokens;
    private final String keepAlive;

    @SuppressWarnings("java:S107")
    public LugiaOllamaProperties(
            @Value("${nord-tool.lugia.ollama.url:}") String url,
            @Value("${nord-tool.lugia.ollama.modelos:}") String modelos,
            @Value("${nord-tool.lugia.ollama.cf-access-client-id:}") String cfAccessId,
            @Value("${nord-tool.lugia.ollama.cf-access-client-secret:}") String cfAccessSecret,
            @Value("${nord-tool.lugia.ollama.token:}") String token,
            @Value("${nord-tool.lugia.ollama.max-simultaneos:1}") int maxSimultaneos,
            @Value("${nord-tool.lugia.ollama.fila-max:4}") int filaMax,
            @Value("${nord-tool.lugia.ollama.espera-max-s:60}") long esperaMaxS,
            @Value("${nord-tool.lugia.ollama.primeiro-pedaco-s:90}") long primeiroPedacoS,
            @Value("${nord-tool.lugia.ollama.timeout-s:300}") long timeoutS,
            @Value("${nord-tool.lugia.ollama.num-ctx:8192}") int numCtx,
            @Value("${nord-tool.lugia.ollama.max-tokens:1024}") int maxTokens,
            @Value("${nord-tool.lugia.ollama.keep-alive:30m}") String keepAlive) {
        this.url = limpo(url).replaceAll("/+$", "");
        this.modelos = Arrays.stream(limpo(modelos).split(",")).map(String::trim).filter(v -> !v.isEmpty()).collect(Collectors.toList());
        this.cfAccessId = limpo(cfAccessId);
        this.cfAccessSecret = limpo(cfAccessSecret);
        this.token = limpo(token);
        this.maxSimultaneos = entre(maxSimultaneos, 1, 8);
        this.filaMax = entre(filaMax, 0, 50);
        this.esperaMaxMs = entre(esperaMaxS, 5, 600) * 1000;
        this.primeiroPedacoMs = entre(primeiroPedacoS, 5, 600) * 1000;
        this.totalMs = entre(timeoutS, 10, 1800) * 1000;
        this.numCtx = entre(numCtx, 1024, 131_072);
        this.maxTokens = entre(maxTokens, 64, 16_384);
        this.keepAlive = limpo(keepAlive).isEmpty() ? "30m" : limpo(keepAlive);
    }

    /** Configurada com URL e modelo. */
    public boolean configurada() { return !url.isEmpty() && !modelos.isEmpty(); }

    /** Pode ser usada: configurada e, fora de localhost, com HTTPS e proteção (nunca fala com a máquina exposta). */
    public boolean segura() {
        if (!configurada()) return false;
        URI u;
        try {
            u = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (local(u.getHost())) return true;
        boolean protegida = !cfAccessId.isEmpty() && !cfAccessSecret.isEmpty() || !token.isEmpty();
        return "https".equalsIgnoreCase(u.getScheme()) && protegida;
    }

    /** localhost ou um IP de loopback escrito na URL (sem consultar DNS para nomes). */
    static boolean local(String host) {
        if (host == null || host.isEmpty()) return false;
        if ("localhost".equalsIgnoreCase(host)) return true;
        String ip = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (!ip.matches("[0-9.]+|[0-9a-fA-F:]+")) return false;
        try {
            return InetAddress.getByName(ip).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** Cabeçalhos de acesso ao túnel (service token do Cloudflare Access e/ou Bearer). */
    public Map<String, String> cabecalhos() {
        Map<String, String> h = new LinkedHashMap<>();
        if (!cfAccessId.isEmpty() && !cfAccessSecret.isEmpty()) {
            h.put("CF-Access-Client-Id", cfAccessId);
            h.put("CF-Access-Client-Secret", cfAccessSecret);
        }
        if (!token.isEmpty()) h.put("Authorization", "Bearer " + token);
        return Collections.unmodifiableMap(h);
    }

    private static String limpo(String v) { return v == null ? "" : v.trim(); }

    private static int entre(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private static long entre(long v, long min, long max) { return Math.max(min, Math.min(max, v)); }

    public String getUrl() { return url; }
    public List<String> getModelos() { return modelos; }
    public int getMaxSimultaneos() { return maxSimultaneos; }
    public int getFilaMax() { return filaMax; }
    public long getEsperaMaxMs() { return esperaMaxMs; }
    public long getPrimeiroPedacoMs() { return primeiroPedacoMs; }
    public long getTotalMs() { return totalMs; }
    public int getNumCtx() { return numCtx; }
    public int getMaxTokens() { return maxTokens; }
    public String getKeepAlive() { return keepAlive; }
}
