package br.com.nord_tool_backend.service.marketing;

/** A rede recusou ou não respondeu; a mensagem é para a pessoa (sem tokens nem chaves). */
public class FalhaRedeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FalhaRedeException(String mensagem) {
        super(mensagem);
    }

    public FalhaRedeException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
