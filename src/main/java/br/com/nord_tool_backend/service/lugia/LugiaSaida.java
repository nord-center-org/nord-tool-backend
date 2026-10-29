package br.com.nord_tool_backend.service.lugia;

/**
 * Para onde a resposta da Lugia é transmitida (SSE no controller). {@link #abrir()} é chamado só depois de
 * validar o pedido: erros antes disso viram a resposta HTTP de erro normal.
 */
public interface LugiaSaida {

    void abrir();

    void conversa(Long idConversa);

    void texto(String delta);

    /** Passo do agente (ferramenta usada), mostrado como linha de progresso. */
    void ferramenta(String descricao);

    /** Provedor e modelo que vão responder (no modo Automático muda quando um deles atinge o limite). */
    default void provedor(String id, String modelo) {
    }

    void fim(String motivo);

    void erro(String mensagem);
}
