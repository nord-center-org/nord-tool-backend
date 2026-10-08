package br.com.nord_tool_backend.controller.response;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

/** Respostas do Financeiro: dados financeiros pessoais, nunca em cache (Cache-Control: no-store). */
public interface FinanceiroResponse extends BaseResponse {

    @Override
    default <T> ResponseEntity<ApiResponseBody<T>> ok(T body) {
        return ResponseEntity.status(NordHttpEnum.HTTP_200.getStatus())
                .cacheControl(CacheControl.noStore())
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_200, NordHttpEnum.HTTP_200.getMensagem(), body));
    }

    @Override
    default <T> ResponseEntity<ApiResponseBody<T>> created(T body) {
        return ResponseEntity.status(NordHttpEnum.HTTP_201.getStatus())
                .cacheControl(CacheControl.noStore())
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_201, NordHttpEnum.HTTP_201.getMensagem(), body));
    }

    @Override
    default <T> ResponseEntity<ApiResponseBody<T>> noContent() {
        return ResponseEntity.status(NordHttpEnum.HTTP_204.getStatus())
                .cacheControl(CacheControl.noStore())
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_204, NordHttpEnum.HTTP_204.getMensagem(), null));
    }
}
