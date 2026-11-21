package br.com.nord_tool_backend.config;

import br.com.nord_tool_backend.websocket.GameSalaWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

/** WebSocket das salas de Games, nas mesmas origens do CORS. Mensagens pequenas e conexões paradas caem em 2 min. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String CAMINHO_SALAS = "/api/v1/nord-tool/games/ws";

    private final GameSalaWebSocketHandler handler;
    private final String[] origens;

    public WebSocketConfig(GameSalaWebSocketHandler handler, @Value("${nord-tool.cors.allowed-origins:}") String[] origens) {
        this.handler = handler;
        this.origens = Arrays.stream(origens).map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, CAMINHO_SALAS).setAllowedOrigins(origens);
    }

    @Bean
    public ServletServerContainerFactoryBean containerDoWebSocket() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxTextMessageBufferSize(8 * 1024);
        c.setMaxBinaryMessageBufferSize(1024);
        c.setMaxSessionIdleTimeout(120_000L);
        return c;
    }
}
