package br.com.nord_tool_backend.websocket;

import br.com.nord_tool_backend.service.DrumcamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket do Drumcam pela internet. Só traduz a conexão em {@link CanalSala}; a regra (bilhete, quem fala com quem,
 * limites) fica no {@link DrumcamService}. O bilhete vem na query ({@code ?bilhete=}), como nas salas de Games.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrumcamWebSocketHandler extends TextWebSocketHandler {

    private static final int LIMITE_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER = 4 * 1024 * 1024;
    private static final int MAX_MOTIVO = 100;

    private final DrumcamService servico;
    private final Map<String, CanalSala> canais = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, LIMITE_ENVIO_MS, LIMITE_BUFFER);
        CanalSala canal = new Canal(segura);
        canais.put(session.getId(), canal);
        servico.conectar(bilhete(session.getUri()), canal);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        CanalSala canal = canais.get(session.getId());
        if (canal != null) servico.receber(canal, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        CanalSala canal = canais.remove(session.getId());
        if (canal != null) servico.desconectar(canal);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Erro de transporte na Drumcam: {}", exception.getMessage());
    }

    private static String bilhete(URI uri) {
        return uri == null ? null : UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("bilhete");
    }

    /** A conexão vista pelo serviço. */
    private static final class Canal implements CanalSala {
        private final WebSocketSession sessao;

        private Canal(WebSocketSession sessao) {
            this.sessao = sessao;
        }

        @Override
        public String id() {
            return sessao.getId();
        }

        @Override
        public void enviar(String json) {
            if (!sessao.isOpen()) return;
            try {
                sessao.sendMessage(new TextMessage(json.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException | IllegalStateException e) {
                log.debug("Não deu para enviar para a Drumcam: {}", e.getMessage());
            }
        }

        @Override
        public void fechar(String motivo) {
            if (!sessao.isOpen()) return;
            String curto = motivo.length() > MAX_MOTIVO ? motivo.substring(0, MAX_MOTIVO) : motivo;
            try {
                sessao.close(CloseStatus.POLICY_VIOLATION.withReason(curto));
            } catch (IOException e) {
                log.debug("Não deu para fechar a conexão da Drumcam: {}", e.getMessage());
            }
        }
    }
}
