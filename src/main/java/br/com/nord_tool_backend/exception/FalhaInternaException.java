package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 500: falha de infraestrutura. A mensagem pública é sempre genérica; o detalhe vai na causa (log). */
public class FalhaInternaException extends NordException {

    public static final String CD_ERRO = "FALHA_INTERNA";

    public FalhaInternaException(Throwable causa) {
        super(NordHttpEnum.HTTP_500, CD_ERRO, NordHttpEnum.HTTP_500.getMensagem(), causa);
    }
}
