package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 403: autenticado, mas sem permissão para a ação. */
public class AcessoNegadoException extends NordException {

    public AcessoNegadoException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_403, cdErro, mensagem, null);
    }

    public AcessoNegadoException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_403, cdErro, mensagem, causa);
    }
}
