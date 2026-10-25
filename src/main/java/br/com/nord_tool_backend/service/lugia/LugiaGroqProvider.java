package br.com.nord_tool_backend.service.lugia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Groq: modelos abertos (Llama, GPT-OSS, Qwen) muito rápidos, com plano gratuito sem cartão (GROQ_API_KEY). */
@Component
@Order(4)
public class LugiaGroqProvider extends LugiaChatCompletionsProvider {

    static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    private final LugiaProperties props;

    public LugiaGroqProvider(LugiaProperties props, ObjectMapper json) {
        super(json);
        this.props = props;
    }

    @Override public String id() { return "groq"; }
    @Override public String nome() { return "Groq"; }
    @Override public List<String> modelos() { return props.getModelosGroq(); }
    @Override String url() { return URL; }
    @Override String chave() { return props.getGroqApiKey(); }
}
