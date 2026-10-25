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

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** ChatGPT pela Responses API da OpenAI (HTTP com SSE). Imagens e PDF vão como data URL. */
@Component @Slf4j
@Order(2)
public class LugiaOpenAiProvider implements LugiaIaProvider {

    static final String URL = "https://api.openai.com/v1/responses";

    private final LugiaProperties props;
    private final ObjectMapper json;

    public LugiaOpenAiProvider(LugiaProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public String id() { return "openai"; }

    @Override
    public String nome() { return "ChatGPT"; }

    @Override
    public List<String> modelos() { return props.getModelosOpenai(); }

    @Override
    public boolean disponivel() {
        return !props.getOpenaiApiKey().isEmpty() && !props.getModelosOpenai().isEmpty();
    }

    @Override
    public LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto) {
        ObjectNode corpo = json.createObjectNode().put("model", modelo).put("instructions", sistema).put("stream", true);
        corpo.set("input", entrada(historico));
        StringBuilder texto = new StringBuilder();
        String[] motivo = {"completed"};
        Integer[] uso = {null, null};
        String[] falha = {null};
        try {
            LugiaSseHttp.postarComRetentativa(URL, Collections.singletonMap("Authorization", "Bearer " + props.getOpenaiApiKey()),
                    json.writeValueAsString(corpo), dado -> {
                        JsonNode ev = ler(dado);
                        String tipo = ev.path("type").asText();
                        if ("response.output_text.delta".equals(tipo)) {
                            String delta = ev.path("delta").asText("");
                            texto.append(delta);
                            aoTexto.accept(delta);
                        } else if ("response.completed".equals(tipo) || "response.incomplete".equals(tipo)) {
                            motivo[0] = ev.path("response").path("status").asText(motivo[0]);
                            JsonNode u = ev.path("response").path("usage");
                            if (u.isObject()) {
                                uso[0] = u.path("input_tokens").asInt();
                                uso[1] = u.path("output_tokens").asInt();
                            }
                        } else if ("response.failed".equals(tipo) || "error".equals(tipo)) {
                            falha[0] = ev.path("response").path("error").path("message").asText(ev.path("message").asText("falha"));
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LugiaFalhaException("Resposta cancelada.", e);
        } catch (Exception e) {
            log.warn("OpenAI falhou: {}", e.getMessage());
            throw new LugiaFalhaException(LugiaSseHttp.mensagemErro("ChatGPT", e), e);
        }
        if (falha[0] != null) {
            log.warn("OpenAI devolveu erro no stream: {}", falha[0]);
            throw new LugiaFalhaException("O ChatGPT não conseguiu responder.");
        }
        return new LugiaResposta(texto.toString(), null, motivo[0], uso[0], uso[1]);
    }

    /** Histórico neutro → itens de entrada da Responses API. */
    ArrayNode entrada(List<LugiaMensagemDto> historico) {
        ArrayNode itens = json.createArrayNode();
        for (LugiaMensagemDto m : historico) {
            ObjectNode item = itens.addObject();
            if (LugiaMensagemDto.ASSISTENTE.equals(m.getPapel())) {
                item.put("role", "assistant").put("content", LugiaClaudeProvider.textoOuReticencias(m.getPartes()));
                continue;
            }
            item.put("role", "user");
            ArrayNode conteudo = item.putArray("content");
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.IMAGEM.equals(p.getTipo())) {
                    conteudo.addObject().put("type", "input_image").put("detail", "auto")
                            .put("image_url", "data:" + p.getMime() + ";base64," + p.getBase64());
                } else if (LugiaParteDto.PDF.equals(p.getTipo())) {
                    conteudo.addObject().put("type", "input_file").put("filename", p.getNome() == null ? "documento.pdf" : p.getNome())
                            .put("file_data", "data:application/pdf;base64," + p.getBase64());
                }
            }
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.TEXTO.equals(p.getTipo()) && p.getTexto() != null && !p.getTexto().trim().isEmpty()) {
                    conteudo.addObject().put("type", "input_text").put("text", p.getTexto());
                }
            }
            if (conteudo.size() == 0) conteudo.addObject().put("type", "input_text").put("text", "…");
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
