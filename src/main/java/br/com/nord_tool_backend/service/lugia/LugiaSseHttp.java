package br.com.nord_tool_backend.service.lugia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/** POST com resposta em Server-Sent Events (OpenAI e Gemini): entrega o conteúdo de cada linha "data:". */
final class LugiaSseHttp {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private LugiaSseHttp() {
    }

    /** Resultado HTTP sem sucesso: status e corpo (para log, nunca para a pessoa). */
    static final class ErroHttp extends IOException {
        final int status;

        ErroHttp(int status, String corpo) {
            super("HTTP " + status + ": " + (corpo.length() > 500 ? corpo.substring(0, 500) : corpo));
            this.status = status;
        }
    }

    static void postar(String url, Map<String, String> cabecalhos, String corpo, Consumer<String> aoDado)
            throws IOException, InterruptedException {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));
        cabecalhos.forEach(pedido::header);
        HttpResponse<InputStream> resposta = HTTP.send(pedido.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(resposta.body(), StandardCharsets.UTF_8))) {
            if (resposta.statusCode() / 100 != 2) {
                StringBuilder erro = new StringBuilder();
                String linha;
                while ((linha = leitor.readLine()) != null && erro.length() < 2000) erro.append(linha);
                throw new ErroHttp(resposta.statusCode(), erro.toString());
            }
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.startsWith("data:")) {
                    String dado = linha.substring(5).trim();
                    if (!dado.isEmpty() && !"[DONE]".equals(dado)) aoDado.accept(dado);
                }
            }
        }
    }

    static String mensagemErro(String provedor, Exception e) {
        if (e instanceof ErroHttp) {
            int status = ((ErroHttp) e).status;
            if (status == 429) return "Limite de uso do " + provedor + " atingido; tente em instantes.";
            boolean chave = e.getMessage() != null && e.getMessage().contains("API key");
            if (status == 401 || status == 403 || chave) return "Chave do " + provedor + " inválida no servidor.";
            if (status == 400 || status == 404) return "O " + provedor + " recusou o pedido (verifique o modelo e os anexos).";
            return "Falha no " + provedor + " (" + status + ").";
        }
        return "Não foi possível falar com o " + provedor + " agora.";
    }
}
