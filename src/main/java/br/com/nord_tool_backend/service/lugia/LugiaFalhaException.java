package br.com.nord_tool_backend.service.lugia;

/**
 * Falha ao falar com um provedor de IA ou com o agente, com mensagem própria para a pessoa (sem detalhes
 * internos nem segredos). Acontece no meio da transmissão, por isso não é uma NordException.
 */
public class LugiaFalhaException extends RuntimeException {

    /** Quanto tempo o provedor pediu para esperar (limite de uso ou sobrecarga); 0 quando não informou. */
    private final long pausaSegundos;

    public LugiaFalhaException(String mensagem) {
        this(mensagem, null, 0);
    }

    public LugiaFalhaException(String mensagem, Throwable causa) {
        this(mensagem, causa, 0);
    }

    public LugiaFalhaException(String mensagem, Throwable causa, long pausaSegundos) {
        super(mensagem, causa);
        this.pausaSegundos = pausaSegundos;
    }

    public long getPausaSegundos() { return pausaSegundos; }
}
