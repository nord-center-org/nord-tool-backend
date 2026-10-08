package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 422: regra de negócio violada por uma entrada bem formada. */
public class NegocioException extends NordException {

    public NegocioException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_422, cdErro, mensagem, null);
    }

    public NegocioException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_422, cdErro, mensagem, causa);
    }
}
