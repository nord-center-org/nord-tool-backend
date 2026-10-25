package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base para provedores com API compatível com Chat Completions da OpenAI (Groq, OpenRouter), muitos com plano
 * gratuito. Só texto: os anexos viram um aviso, porque boa parte desses modelos não lê imagem nem PDF.
 */
@Slf4j
abstract class LugiaChatCompletionsProvider implements LugiaIaProvider {

    private final ObjectMapper json;

    LugiaChatCompletionsProvider(ObjectMapper json) {
        this.json = json;
    }

    /** Endereço completo de /chat/completions. */
    abstract String url();

    abstract String chave();

    @Override
    public boolean disponivel() {
        return !chave().isEmpty() && !modelos().isEmpty();
    }

    @Override
    public LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto) {
        ObjectNode corpo = json.createObjectNode().put("model", modelo).put("stream", true);
        corpo.putObject("stream_options").put("include_usage", true);
        corpo.set("messages", mensagens(sistema, historico));
        StringBuilder texto = new StringBuilder();
        String[] motivo = {"stop"};
        Integer[] uso = {null, null};
        try {
            LugiaSseHttp.postarComRetentativa(url(), Collections.singletonMap("Authorization", "Bearer " + chave()),
                    json.writeValueAsString(corpo), dado -> {
                        JsonNode ev = ler(dado);
                        JsonNode escolha = ev.path("choices").path(0);
                        String delta = escolha.path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            texto.append(delta);
                            aoTexto.accept(delta);
                        }
                        if (escolha.hasNonNull("finish_reason")) motivo[0] = escolha.path("finish_reason").asText();
                        JsonNode u = ev.path("usage");
                        if (u.isObject()) {
                            uso[0] = u.path("prompt_tokens").asInt();
                            uso[1] = u.path("completion_tokens").asInt();
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LugiaFalhaException("Resposta cancelada.", e);
        } catch (Exception e) {
            log.warn("{} falhou: {}", nome(), e.getMessage());
            throw new LugiaFalhaException(LugiaSseHttp.mensagemErro(nome(), e), e);
        }
        return new LugiaResposta(texto.toString(), null, motivo[0], uso[0], uso[1]);
    }

    /** Histórico neutro → mensagens de Chat Completions (sistema primeiro, anexos como aviso no texto). */
    ArrayNode mensagens(String sistema, List<LugiaMensagemDto> historico) {
        ArrayNode itens = json.createArrayNode();
        itens.addObject().put("role", "system").put("content", sistema);
        for (LugiaMensagemDto m : historico) {
            boolean assistente = LugiaMensagemDto.ASSISTENTE.equals(m.getPapel());
            StringBuilder conteudo = new StringBuilder();
            if (!assistente) {
                for (LugiaParteDto p : m.getPartes()) {
                    if (!LugiaParteDto.TEXTO.equals(p.getTipo())) {
                        conteudo.append("[Anexo \"").append(p.getNome() == null ? p.getTipo() : p.getNome())
                                .append("\" não enviado: este modelo só lê texto. Para analisar imagens e PDFs, use o Gemini ou o Claude.]\n");
                    }
                }
            }
            String texto = LugiaClaudeProvider.textoOuReticencias(m.getPartes());
            if (!"…".equals(texto) || conteudo.length() == 0) conteudo.append(texto);
            itens.addObject().put("role", assistente ? "assistant" : "user").put("content", conteudo.toString().trim());
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
