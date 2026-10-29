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
        /** Cabeçalho Retry-After em segundos; 0 quando ausente ou em formato de data. */
        final long esperarSegundos;

        ErroHttp(int status, String corpo) {
            this(status, corpo, 0);
        }

        ErroHttp(int status, String corpo, long esperarSegundos) {
            super("HTTP " + status + ": " + (corpo.length() > 500 ? corpo.substring(0, 500) : corpo));
            this.status = status;
            this.esperarSegundos = esperarSegundos;
        }
    }

    /** Esperas antes de tentar de novo quando o provedor está sobrecarregado (429/500/502/503/504). */
    static final long[] ESPERAS_MS = {1500, 4000};

    /** Acima disso o Retry-After indica cota esgotada (por hora ou dia): esperar aqui só atrasaria a resposta. */
    static final long ESPERA_MAXIMA_SEGUNDOS = 10;

    /** Pausa sugerida quando o provedor recusa por limite (429) sem dizer por quanto tempo. */
    static final long PAUSA_LIMITE_SEGUNDOS = 60;

    /** Pausa sugerida quando o provedor está sobrecarregado (5xx). */
    static final long PAUSA_SOBRECARGA_SEGUNDOS = 30;

    /**
     * Como {@link #postar}, mas repete quando o provedor responde sobrecarregado. Seguro porque o erro chega
     * no status, antes de qualquer pedaço da resposta ter sido entregue.
     */
    static void postarComRetentativa(String url, Map<String, String> cabecalhos, String corpo, Consumer<String> aoDado)
            throws IOException, InterruptedException {
        for (int tentativa = 0; ; tentativa++) {
            try {
                postar(url, cabecalhos, corpo, aoDado);
                return;
            } catch (ErroHttp e) {
                if (!temporario(e.status) || tentativa >= ESPERAS_MS.length || e.esperarSegundos > ESPERA_MAXIMA_SEGUNDOS) throw e;
                Thread.sleep(ESPERAS_MS[tentativa]);
            }
        }
    }

    static boolean temporario(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
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
                throw new ErroHttp(resposta.statusCode(), erro.toString(), segundos(resposta.headers().firstValue("Retry-After").orElse("")));
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

    static long segundos(String retryAfter) {
        try {
            return Math.max(0, Long.parseLong(retryAfter.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Falha para a pessoa, com a pausa que o modo Automático deve dar a este provedor antes de usá-lo de novo. */
    static LugiaFalhaException falha(String provedor, Exception e) {
        long pausa = 0;
        if (e instanceof ErroHttp) {
            ErroHttp erro = (ErroHttp) e;
            if (erro.status == 429) pausa = Math.max(erro.esperarSegundos, PAUSA_LIMITE_SEGUNDOS);
            else if (temporario(erro.status)) pausa = Math.max(erro.esperarSegundos, PAUSA_SOBRECARGA_SEGUNDOS);
        }
        return new LugiaFalhaException(mensagemErro(provedor, e), e, pausa);
    }

    static String mensagemErro(String provedor, Exception e) {
        if (e instanceof ErroHttp) {
            int status = ((ErroHttp) e).status;
            if (status == 429) return "Limite de uso do " + provedor + " atingido; tente em instantes.";
            boolean chave = e.getMessage() != null && e.getMessage().contains("API key");
            if (status == 401 || status == 403 || chave) return "Chave do " + provedor + " inválida no servidor.";
            if (status == 400 || status == 404) return "O " + provedor + " recusou o pedido (verifique o modelo e os anexos).";
            if (status == 503 || status == 502 || status == 504 || status == 500) {
                return "O " + provedor + " está sobrecarregado agora (comum no plano gratuito). Tente de novo em instantes ou troque de modelo.";
            }
            return "Falha no " + provedor + " (" + status + ").";
        }
        return "Não foi possível falar com o " + provedor + " agora.";
    }
}
