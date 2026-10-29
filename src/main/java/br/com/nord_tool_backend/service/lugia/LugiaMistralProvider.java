package br.com.nord_tool_backend.service.lugia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Mistral: plano gratuito "Experiment" sem cartão (MISTRAL_API_KEY); os pedidos podem ser usados para treino. */
@Component
@Order(6)
public class LugiaMistralProvider extends LugiaChatCompletionsProvider {

    static final String URL = "https://api.mistral.ai/v1/chat/completions";

    private final LugiaProperties props;

    public LugiaMistralProvider(LugiaProperties props, ObjectMapper json) {
        super(json);
        this.props = props;
    }

    @Override public String id() { return "mistral"; }
    @Override public String nome() { return "Mistral"; }
    @Override public List<String> modelos() { return props.getModelosMistral(); }
    @Override String url() { return URL; }
    @Override String chave() { return props.getMistralApiKey(); }
}
