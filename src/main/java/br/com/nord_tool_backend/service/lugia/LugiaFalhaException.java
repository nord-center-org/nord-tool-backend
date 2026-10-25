package br.com.nord_tool_backend.service.lugia;

/**
 * Falha ao falar com um provedor de IA ou com o agente, com mensagem própria para a pessoa (sem detalhes
 * internos nem segredos). Acontece no meio da transmissão, por isso não é uma NordException.
 */
public class LugiaFalhaException extends RuntimeException {

    public LugiaFalhaException(String mensagem) {
        super(mensagem);
    }

    public LugiaFalhaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
