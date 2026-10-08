package br.com.nord_tool_backend.controller.response;

import br.com.nord_tool_backend.utils.ApiResponseMessageUtils;
import org.springframework.http.HttpStatus;

public enum NordHttpEnum {

    HTTP_200(HttpStatus.OK, ApiResponseMessageUtils.RESPONSE_200),
    HTTP_201(HttpStatus.CREATED, ApiResponseMessageUtils.RESPONSE_201),
    HTTP_204(HttpStatus.NO_CONTENT, ApiResponseMessageUtils.RESPONSE_204),
    HTTP_304(HttpStatus.NOT_MODIFIED, ApiResponseMessageUtils.RESPONSE_304),
    HTTP_400(HttpStatus.BAD_REQUEST, ApiResponseMessageUtils.RESPONSE_400),
    HTTP_401(HttpStatus.UNAUTHORIZED, ApiResponseMessageUtils.RESPONSE_401),
    HTTP_403(HttpStatus.FORBIDDEN, ApiResponseMessageUtils.RESPONSE_403),
    HTTP_404(HttpStatus.NOT_FOUND, ApiResponseMessageUtils.RESPONSE_404),
    HTTP_405(HttpStatus.METHOD_NOT_ALLOWED, ApiResponseMessageUtils.RESPONSE_405),
    HTTP_409(HttpStatus.CONFLICT, ApiResponseMessageUtils.RESPONSE_409),
    HTTP_413(HttpStatus.PAYLOAD_TOO_LARGE, ApiResponseMessageUtils.RESPONSE_413),
    HTTP_422(HttpStatus.UNPROCESSABLE_ENTITY, ApiResponseMessageUtils.RESPONSE_422),
    HTTP_429(HttpStatus.TOO_MANY_REQUESTS, ApiResponseMessageUtils.RESPONSE_429),
    HTTP_500(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponseMessageUtils.RESPONSE_500);

    private final HttpStatus status;
    private final String mensagem;

    NordHttpEnum(HttpStatus status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }
}
