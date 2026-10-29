package br.com.nord_tool_backend.service.lugia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** NVIDIA (build.nvidia.com): modelos abertos gratuitos para teste, cerca de 40 pedidos por minuto (NVIDIA_API_KEY). */
@Component
@Order(7)
public class LugiaNvidiaProvider extends LugiaChatCompletionsProvider {

    static final String URL = "https://integrate.api.nvidia.com/v1/chat/completions";

    private final LugiaProperties props;

    public LugiaNvidiaProvider(LugiaProperties props, ObjectMapper json) {
        super(json);
        this.props = props;
    }

    @Override public String id() { return "nvidia"; }
    @Override public String nome() { return "NVIDIA"; }
    @Override public List<String> modelos() { return props.getModelosNvidia(); }
    @Override String url() { return URL; }
    @Override String chave() { return props.getNvidiaApiKey(); }
}
