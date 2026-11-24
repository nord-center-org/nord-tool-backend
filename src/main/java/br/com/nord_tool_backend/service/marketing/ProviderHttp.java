package br.com.nord_tool_backend.service.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/** Chamadas HTTP comuns às redes: JSON, formulário e erros traduzidos (sem tokens na mensagem). */
abstract class ProviderHttp implements RedeSocialProvider {

    protected final RestTemplate http;
    protected final ObjectMapper json = new ObjectMapper();

    protected ProviderHttp(RestTemplate http) {
        this.http = http;
    }

    protected JsonNode get(String url, String bearer) {
        return trocar(url, HttpMethod.GET, bearer, null, null);
    }

    protected JsonNode postForm(String url, Map<String, String> campos) {
        MultiValueMap<String, String> corpo = new LinkedMultiValueMap<>();
        campos.forEach(corpo::add);
        return trocar(url, HttpMethod.POST, null, corpo, MediaType.APPLICATION_FORM_URLENCODED);
    }

    protected JsonNode postJson(String url, String bearer, Object corpo) {
        return trocar(url, HttpMethod.POST, bearer, corpo, MediaType.APPLICATION_JSON);
    }

    protected JsonNode trocar(String url, HttpMethod metodo, String bearer, Object corpo, MediaType tipo) {
        HttpHeaders h = new HttpHeaders();
        if (bearer != null) h.setBearerAuth(bearer);
        if (tipo != null) h.setContentType(tipo);
        h.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<String> r = http.exchange(URI.create(url), metodo, new HttpEntity<>(corpo, h), String.class);
            return ler(r.getBody());
        } catch (RestClientResponseException e) {
            // Sem a causa: a mensagem da biblioteca HTTP pode trazer a URL, e algumas redes põem o token nela.
            throw new FalhaRedeException(rede().getNome() + " recusou: " + motivo(e.getResponseBodyAsString(), e.getRawStatusCode()));
        } catch (RestClientException e) {
            throw new FalhaRedeException(rede().getNome() + " não respondeu. Tente de novo em instantes.");
        }
    }

    /** Resposta de um envio de trecho do vídeo. */
    protected static final class RespostaEnvio {
        final int status;
        final String corpo;
        final java.util.Optional<String> local;

        public RespostaEnvio(int status, String corpo, java.util.Optional<String> local) {
            this.status = status;
            this.corpo = corpo;
            this.local = local;
        }
    }

    /**
     * Envia um trecho do arquivo direto do disco (sem carregar o vídeo na memória). Sobrescrito nos testes.
     */
    protected RespostaEnvio enviarTrecho(String url, String metodo, Map<String, String> cabecalhos, java.nio.file.Path arquivo,
                                         long inicio, long tamanho) {
        try {
            java.net.http.HttpRequest.Builder b = java.net.http.HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofMinutes(10))
                    .method(metodo, java.net.http.HttpRequest.BodyPublishers.fromPublisher(
                            java.net.http.HttpRequest.BodyPublishers.ofInputStream(() -> trecho(arquivo, inicio, tamanho)), tamanho));
            cabecalhos.forEach(b::header);
            java.net.http.HttpResponse<String> r = clienteEnvio().send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            return new RespostaEnvio(r.statusCode(), r.body(), r.headers().firstValue("Location"));
        } catch (IOException e) {
            throw new FalhaRedeException(rede().getNome() + ": falhou ao enviar o vídeo. Tente de novo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaRedeException(rede().getNome() + ": envio interrompido.");
        }
    }

    private static java.net.http.HttpClient cliente;

    private static synchronized java.net.http.HttpClient clienteEnvio() {
        if (cliente == null) {
            cliente = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(20))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
        }
        return cliente;
    }

    private static java.io.InputStream trecho(java.nio.file.Path arquivo, long inicio, long tamanho) {
        try {
            java.io.InputStream in = java.nio.file.Files.newInputStream(arquivo);
            long pulado = in.skip(inicio);
            while (pulado < inicio) {
                long p = in.skip(inicio - pulado);
                if (p <= 0) break;
                pulado += p;
            }
            return new LimitadoInputStream(in, tamanho);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** Lê no máximo {@code restante} bytes. */
    private static final class LimitadoInputStream extends java.io.FilterInputStream {
        private long restante;

        LimitadoInputStream(java.io.InputStream in, long restante) {
            super(in);
            this.restante = restante;
        }

        @Override
        public int read() throws IOException {
            if (restante <= 0) return -1;
            int b = super.read();
            if (b >= 0) restante--;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (restante <= 0) return -1;
            int n = super.read(buf, off, (int) Math.min(len, restante));
            if (n > 0) restante -= n;
            return n;
        }
    }

    /** Pausa entre consultas de "já terminou de processar?". Sobrescrita nos testes. */
    protected void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaRedeException(rede().getNome() + ": envio interrompido.");
        }
    }

    protected JsonNode ler(String corpo) {
        if (corpo == null || corpo.isBlank()) return json.createObjectNode();
        try {
            return json.readTree(corpo);
        } catch (IOException e) {
            return json.createObjectNode();
        }
    }

    /** Mensagem de erro que a rede mandou (campos comuns das três APIs), cortada e sem nada além do texto. */
    protected String motivo(String corpo, int status) {
        JsonNode n = ler(corpo);
        String[] caminhos = {"/error/message", "/error_description", "/error/errors/0/message", "/error/error_user_msg", "/error/code", "/message"};
        for (String c : caminhos) {
            JsonNode v = n.at(c);
            if (v.isTextual() && !v.asText().isBlank()) return corta(v.asText());
        }
        JsonNode e = n.path("error");
        if (e.isTextual()) return corta(e.asText());
        return "HTTP " + status;
    }

    protected static String corta(String s) {
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    protected static LocalDateTime expira(JsonNode n, String campo) {
        long s = n.path(campo).asLong(0);
        return s > 0 ? LocalDateTime.now().plusSeconds(s) : null;
    }

    protected static Long numero(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        if (n.isNumber()) return n.asLong();
        if (n.isTextual()) {
            try {
                return Long.parseLong(n.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    protected static String texto(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? null : n.asText();
    }

    protected static String enc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
