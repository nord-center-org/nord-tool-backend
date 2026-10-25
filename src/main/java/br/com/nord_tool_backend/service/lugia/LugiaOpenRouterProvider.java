package br.com.nord_tool_backend.service.lugia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** OpenRouter: um catálogo de modelos com variantes gratuitas (ids terminados em ":free"), via OPENROUTER_API_KEY. */
@Component
@Order(5)
public class LugiaOpenRouterProvider extends LugiaChatCompletionsProvider {

    static final String URL = "https://openrouter.ai/api/v1/chat/completions";

    private final LugiaProperties props;

    public LugiaOpenRouterProvider(LugiaProperties props, ObjectMapper json) {
        super(json);
        this.props = props;
    }

    @Override public String id() { return "openrouter"; }
    @Override public String nome() { return "OpenRouter"; }
    @Override public List<String> modelos() { return props.getModelosOpenrouter(); }
    @Override String url() { return URL; }
    @Override String chave() { return props.getOpenrouterApiKey(); }
}
