package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LugiaOllamaProviderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer servidor;

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
    }

    private String subir(int status, String corpo, AtomicReference<String> recebido, AtomicReference<String> segredo) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/chat", troca -> {
            if (recebido != null) recebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (segredo != null) segredo.set(troca.getRequestHeaders().getFirst("CF-Access-Client-Secret"));
            byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, b.length);
            try (OutputStream o = troca.getResponseBody()) { o.write(b); }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static LugiaOllamaProperties props(String url, String id, String segredo) {
        return new LugiaOllamaProperties(url, "qwen2.5:7b", id, segredo, "", 1, 2, 5, 5, 30, 4096, 256, "30m");
    }

    private static List<LugiaMensagemDto> historico() {
        List<LugiaParteDto> partes = new ArrayList<>();
        partes.add(new LugiaParteDto(LugiaParteDto.TEXTO, "Oi", null, null, null));
        return Collections.singletonList(new LugiaMensagemDto(LugiaMensagemDto.USUARIO, null, partes, null));
    }

    @Test
    void transmiteARespostaComCabecalhosELimites() throws Exception {
        AtomicReference<String> recebido = new AtomicReference<>();
        AtomicReference<String> segredo = new AtomicReference<>();
        String url = subir(200, "{\"message\":{\"content\":\"Olá\"}}\n{\"message\":{\"content\":\", Nick\"}}\n"
                + "{\"done\":true,\"done_reason\":\"stop\",\"prompt_eval_count\":12,\"eval_count\":3}\n", recebido, segredo);
        LugiaOllamaProvider p = new LugiaOllamaProvider(props(url, "id", "seg"), json);
        StringBuilder pedacos = new StringBuilder();
        LugiaResposta r = p.conversar("qwen2.5:7b", "Você é a Lugia.", historico(), pedacos::append);
        assertThat(r.getTexto()).isEqualTo("Olá, Nick");
        assertThat(pedacos).hasToString("Olá, Nick");
        assertThat(r.getTokensEntrada()).isEqualTo(12);
        assertThat(r.getTokensSaida()).isEqualTo(3);
        assertThat(segredo.get()).isEqualTo("seg");
        JsonNode corpo = json.readTree(recebido.get());
        assertThat(corpo.path("options").path("num_ctx").asInt()).isEqualTo(4096);
        assertThat(corpo.path("options").path("num_predict").asInt()).isEqualTo(256);
        assertThat(corpo.path("keep_alive").asText()).isEqualTo("30m");
        assertThat(corpo.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(p.protetor().ocupados()).isZero();
    }

    @Test
    void modeloNaoInstaladoETunelRecusando() throws Exception {
        String url = subir(404, "{\"error\":\"model \\\"qwen2.5:7b\\\" not found, try pulling it first\"}", null, null);
        LugiaOllamaProvider p = new LugiaOllamaProvider(props(url, "", ""), json);
        assertThatThrownBy(() -> p.conversar("qwen2.5:7b", "s", historico(), t -> { }))
                .isInstanceOf(LugiaFalhaException.class).hasMessageContaining("ollama pull qwen2.5:7b");
        assertThat(p.protetor().circuito()).isEqualTo("fechado");
        parar();

        String negado = subir(403, "Forbidden", null, null);
        LugiaOllamaProvider q = new LugiaOllamaProvider(props(negado, "", ""), json);
        assertThatThrownBy(() -> q.conversar("qwen2.5:7b", "s", historico(), t -> { })).hasMessageContaining("service token");
    }

    @Test
    void maquinaForaAbreODisjuntorERecusaNaHora() {
        LugiaOllamaProvider p = new LugiaOllamaProvider(props("http://127.0.0.1:1", "", ""), json);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> p.conversar("qwen2.5:7b", "s", historico(), t -> { })).hasMessageContaining("máquina desligada");
        }
        assertThat(p.protetor().circuito()).isEqualTo("aberto");
        assertThatThrownBy(() -> p.conversar("qwen2.5:7b", "s", historico(), t -> { })).hasMessageContaining("fora do ar agora");
    }

    @Test
    void soLigaComTunelSeguro() {
        assertThat(props("", "", "").segura()).isFalse();
        assertThat(new LugiaOllamaProvider(props("http://localhost:11434", "", ""), json).disponivel()).isTrue();
        assertThat(new LugiaOllamaProvider(props("http://ollama.exemplo.com", "id", "seg"), json).disponivel()).isFalse();
        assertThat(new LugiaOllamaProvider(props("https://ollama.exemplo.com", "", ""), json).disponivel()).isFalse();
        LugiaOllamaProperties seguro = props("https://ollama.exemplo.com/", "id", "seg");
        assertThat(seguro.segura()).isTrue();
        assertThat(seguro.getUrl()).isEqualTo("https://ollama.exemplo.com");
        assertThat(seguro.cabecalhos()).containsEntry("CF-Access-Client-Id", "id").doesNotContainKey("Authorization");
        assertThat(LugiaOllamaProperties.local("localhost")).isTrue();
        assertThat(LugiaOllamaProperties.local("[::1]")).isTrue();
        assertThat(LugiaOllamaProperties.local("ollama.exemplo.com")).isFalse();
        assertThat(LugiaOllamaProperties.local("8.8.8.8")).isFalse();
        LugiaOllamaProperties limites = new LugiaOllamaProperties("http://localhost:11434", "m", "", "", "t", 99, -1, 1, 1, 1, 10, 1, " ");
        assertThat(limites.getMaxSimultaneos()).isEqualTo(8);
        assertThat(limites.getFilaMax()).isZero();
        assertThat(limites.getKeepAlive()).isEqualTo("30m");
        assertThat(limites.cabecalhos()).containsEntry("Authorization", "Bearer t");
    }
}
