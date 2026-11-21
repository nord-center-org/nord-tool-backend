package br.com.nord_tool_backend.websocket;

/** Uma conexão de uma sala de Games (a tela que hospeda ou um celular). O serviço só conhece isto, não o WebSocket. */
public interface CanalSala {

    String id();

    /** Envia uma mensagem JSON; se a conexão caiu, ignora. */
    void enviar(String json);

    void fechar(String motivo);
}
