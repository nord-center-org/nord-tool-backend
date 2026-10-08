package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 413: conteúdo enviado acima do limite. */
public class ArquivoMuitoGrandeException extends NordException {

    public ArquivoMuitoGrandeException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_413, cdErro, mensagem, null);
    }

    public ArquivoMuitoGrandeException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_413, cdErro, mensagem, causa);
    }
}
