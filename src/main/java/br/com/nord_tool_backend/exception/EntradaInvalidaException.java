package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 400: formato, tamanho ou limite da entrada inválido. */
public class EntradaInvalidaException extends NordException {

    public EntradaInvalidaException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_400, cdErro, mensagem, null);
    }

    public EntradaInvalidaException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_400, cdErro, mensagem, causa);
    }
}
