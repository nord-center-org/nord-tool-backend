package br.com.nord_tool_backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseBody<T> {
    private static final long serialVersionUID = 1L;

    /** Chave do MDC com o identificador de correlação da requisição. */
    public static final String MDC_ID_CORRELACAO = "idCorrelacao";

    private LocalDateTime timestamp;
    private Integer nrStatus;
    private transient T body;
    private String txMensagem;

    /** Código estável do erro (ex.: "LANCAMENTO_VERSAO_CONFLITO"); só em respostas de erro. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String cdErro;

    /** Identificador da requisição, igual ao do log; só em respostas de erro. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String idCorrelacao;

    public ApiResponseBody(NordHttpEnum status, String menssage, final T body) {
        this.timestamp = LocalDateTime.now();
        this.nrStatus = status.getStatus().value();
        this.txMensagem = menssage;
        this.body = body;
    }

    /** Resposta de erro: sem corpo, com código estável e o identificador de correlação da requisição. */
    public static <T> ApiResponseBody<T> erro(NordHttpEnum status, String cdErro, String mensagem) {
        ApiResponseBody<T> resposta = new ApiResponseBody<>(status, mensagem, null);
        resposta.cdErro = cdErro;
        resposta.idCorrelacao = MDC.get(MDC_ID_CORRELACAO);
        return resposta;
    }
}
