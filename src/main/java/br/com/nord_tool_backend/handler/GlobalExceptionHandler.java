package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Converte exceções em respostas padronizadas. A causa técnica (mensagem do banco, do Jackson,
 * de I/O) vai somente para o log; o cliente recebe apenas a mensagem pública.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String MSG_CORPO_INVALIDO = "Corpo da requisição inválido";
    private static final String PACOTE_APLICACAO = "br.com.nord_tool_backend.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse(NordHttpEnum.HTTP_400.getMensagem());

        return ResponseEntity
                .badRequest()
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_400, mensagem, null));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ApiResponseBody<String>> handleValidacaoException(
            ValidacaoException ex) {

        NordHttpEnum status = ex.getHttpEnum();
        if (ex.getException() != null) {
            log.warn("{} ({}): {}", ex.getMenssage(), status.getStatus().value(), ex.getException());
        }

        return ResponseEntity
                .status(status.getStatus())
                .body(new ApiResponseBody<>(status, ex.getMenssage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseBody<String>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        log.debug("Corpo da requisição não pôde ser lido", ex);

        return ResponseEntity
                .badRequest()
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_400, mensagemPublica(ex), null));
    }

    /**
     * Mensagens de validação lançadas pelo próprio código (desserializadores e enums do projeto) são públicas;
     * as do Jackson citam classes internas e são trocadas por uma mensagem com o campo afetado.
     */
    static String mensagemPublica(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof IllegalArgumentException && lancadaPelaAplicacao(causa) && causa.getMessage() != null) {
            return causa.getMessage();
        }
        if (ex.getCause() instanceof JsonMappingException) {
            String campo = ((JsonMappingException) ex.getCause()).getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    .replace(".[", "[");
            if (!campo.isEmpty()) {
                return "Valor inválido no campo '" + campo + "'";
            }
        }
        return MSG_CORPO_INVALIDO;
    }

    private static boolean lancadaPelaAplicacao(Throwable causa) {
        StackTraceElement[] pilha = causa.getStackTrace();
        return pilha.length > 0 && pilha[0].getClassName().startsWith(PACOTE_APLICACAO);
    }
}
