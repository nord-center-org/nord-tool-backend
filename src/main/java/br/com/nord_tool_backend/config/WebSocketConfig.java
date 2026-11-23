package br.com.nord_tool_backend.config;

import br.com.nord_tool_backend.websocket.DrumcamWebSocketHandler;
import br.com.nord_tool_backend.websocket.GameSalaWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

/**
 * WebSockets das salas de Games e do Drumcam, nas mesmas origens do CORS. Conexões paradas caem em 2 min. O buffer
 * do contêiner cabe a maior mensagem do Drumcam (a prévia da câmera); cada serviço limita o tamanho das suas.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String CAMINHO_SALAS = "/api/v1/nord-tool/games/ws";
    public static final String CAMINHO_DRUMCAM = "/api/v1/nord-tool/drumcam/ws";

    private final GameSalaWebSocketHandler handler;
    private final DrumcamWebSocketHandler drumcam;
    private final String[] origens;

    public WebSocketConfig(GameSalaWebSocketHandler handler, DrumcamWebSocketHandler drumcam,
                           @Value("${nord-tool.cors.allowed-origins:}") String[] origens) {
        this.handler = handler;
        this.drumcam = drumcam;
        this.origens = Arrays.stream(origens).map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, CAMINHO_SALAS).setAllowedOrigins(origens);
        // O app Android (a câmera) manda o Origin do próprio NordTool, como o site.
        registry.addHandler(drumcam, CAMINHO_DRUMCAM).setAllowedOrigins(origens);
    }

    @Bean
    public ServletServerContainerFactoryBean containerDoWebSocket() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxTextMessageBufferSize(1536 * 1024);
        c.setMaxBinaryMessageBufferSize(1024);
        c.setMaxSessionIdleTimeout(120_000L);
        return c;
    }
}
