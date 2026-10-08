package br.com.nord_tool_backend.excepetion;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.exception.NordException;

/**
 * Exceção legada, mantida como ponte durante a migração para as subclasses de
 * {@link NordException} (pacote {@code exception}). Novo código não deve usá-la.
 * O terceiro argumento é a causa técnica: vai para o log, nunca para a resposta.
 */
@Deprecated
public class ValidacaoException extends NordException {

    private final String ex;

    public ValidacaoException(NordHttpEnum httpEnum, String menssage, String ex) {
        super(httpEnum, null, menssage, null);
        this.ex = ex;
    }

    public NordHttpEnum getHttpEnum() {
        return getStatus();
    }

    public String getMenssage() {
        return getMessage();
    }

    /** Causa técnica, somente para log. */
    public String getException() {
        return ex;
    }
}
