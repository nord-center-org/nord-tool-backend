package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/**
 * Base das exceções da aplicação. A mensagem é pública (vai para {@code txMensagem});
 * a causa técnica, quando houver, fica só no log. {@code cdErro} é um código estável para o frontend.
 */
public abstract class NordException extends RuntimeException {

    private final NordHttpEnum status;
    private final String cdErro;

    protected NordException(NordHttpEnum status, String cdErro, String mensagemPublica, Throwable causa) {
        super(mensagemPublica, causa);
        this.status = status;
        this.cdErro = cdErro;
    }

    public NordHttpEnum getStatus() {
        return status;
    }

    public String getCdErro() {
        return cdErro;
    }
}
