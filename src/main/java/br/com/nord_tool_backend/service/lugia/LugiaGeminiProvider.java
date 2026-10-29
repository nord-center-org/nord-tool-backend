package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Gemini pela API generateContent do Google (streamGenerateContent com SSE). Anexos vão como inlineData. */
@Component @Slf4j
@Order(3)
public class LugiaGeminiProvider implements LugiaIaProvider {

    static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?alt=sse";

    private final LugiaProperties props;
    private final ObjectMapper json;

    public LugiaGeminiProvider(LugiaProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public String id() { return "gemini"; }

    @Override
    public String nome() { return "Gemini"; }

    @Override
    public List<String> modelos() { return props.getModelosGemini(); }

    @Override
    public boolean disponivel() {
        return !props.getGeminiApiKey().isEmpty() && !props.getModelosGemini().isEmpty();
    }

    /** Chave do Google AI Studio sem faturamento ativo usa o plano gratuito. */
    @Override
    public boolean gratuito() { return true; }

    @Override
    public boolean leAnexos() { return true; }

    @Override
    public LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto) {
        ObjectNode corpo = json.createObjectNode();
        corpo.putObject("systemInstruction").putArray("parts").addObject().put("text", sistema);
        corpo.set("contents", conteudos(historico));
        StringBuilder texto = new StringBuilder();
        String[] motivo = {"STOP"};
        Integer[] uso = {null, null};
        try {
            String url = String.format(URL, URLEncoder.encode(modelo, StandardCharsets.UTF_8));
            LugiaSseHttp.postarComRetentativa(url, Collections.singletonMap("x-goog-api-key", props.getGeminiApiKey()),
                    json.writeValueAsString(corpo), dado -> {
                        JsonNode ev = ler(dado);
                        JsonNode candidato = ev.path("candidates").path(0);
                        for (JsonNode parte : candidato.path("content").path("parts")) {
                            if (parte.has("text") && !parte.path("thought").asBoolean(false)) {
                                String delta = parte.path("text").asText("");
                                texto.append(delta);
                                aoTexto.accept(delta);
                            }
                        }
                        if (candidato.has("finishReason")) motivo[0] = candidato.path("finishReason").asText();
                        JsonNode u = ev.path("usageMetadata");
                        if (u.isObject()) {
                            uso[0] = u.path("promptTokenCount").asInt();
                            uso[1] = u.path("candidatesTokenCount").asInt();
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LugiaFalhaException("Resposta cancelada.", e);
        } catch (Exception e) {
            log.warn("Gemini falhou: {}", e.getMessage());
            throw LugiaSseHttp.falha("Gemini", e);
        }
        return new LugiaResposta(texto.toString(), null, motivo[0], uso[0], uso[1]);
    }

    /** Histórico neutro → contents do Gemini (papel "model" para o assistente). */
    ArrayNode conteudos(List<LugiaMensagemDto> historico) {
        ArrayNode itens = json.createArrayNode();
        for (LugiaMensagemDto m : historico) {
            ObjectNode item = itens.addObject();
            if (LugiaMensagemDto.ASSISTENTE.equals(m.getPapel())) {
                item.put("role", "model").putArray("parts").addObject().put("text", LugiaClaudeProvider.textoOuReticencias(m.getPartes()));
                continue;
            }
            item.put("role", "user");
            ArrayNode partes = item.putArray("parts");
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.IMAGEM.equals(p.getTipo()) || LugiaParteDto.PDF.equals(p.getTipo())) {
                    String mime = LugiaParteDto.PDF.equals(p.getTipo()) ? "application/pdf" : p.getMime();
                    partes.addObject().putObject("inlineData").put("mimeType", mime).put("data", p.getBase64());
                }
            }
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.TEXTO.equals(p.getTipo()) && p.getTexto() != null && !p.getTexto().trim().isEmpty()) {
                    partes.addObject().put("text", p.getTexto());
                }
            }
            if (partes.size() == 0) partes.addObject().put("text", "…");
        }
        return itens;
    }

    private JsonNode ler(String dado) {
        try {
            return json.readTree(dado);
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }
}
