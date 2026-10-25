package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.form.LugiaChatForm;
import br.com.nord_tool_backend.form.LugiaTituloForm;
import br.com.nord_tool_backend.service.LugiaService;
import br.com.nord_tool_backend.service.lugia.LugiaSaida;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/lugia")
@Tag(name = "Lugia", description = "Chat com Claude, ChatGPT e Gemini e agente executor")
@Slf4j
public class LugiaWriteController implements BaseResponse {

    private final LugiaService service;
    private final ObjectMapper json;

    @Operation(summary = "Renomeia a conversa")
    @PatchMapping("/conversas/{id}")
    public ResponseEntity<ApiResponseBody<Void>> renomear(@PathVariable Long id, @Valid @RequestBody LugiaTituloForm form) {
        service.renomear(id, form);
        return noContent();
    }

    @Operation(summary = "Exclui a conversa")
    @DeleteMapping("/conversas/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }

    /**
     * Envia a mensagem e transmite a resposta em Server-Sent Events: conversa, texto, ferramenta, fim, erro.
     * Escreve direto na resposta, na própria thread da requisição (sem async), para manter o usuário autenticado.
     */
    @Operation(summary = "Envia a mensagem e recebe a resposta em tempo real (SSE)")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void chat(@Valid @RequestBody LugiaChatForm form, HttpServletResponse resposta) {
        service.conversar(form, new SaidaSse(resposta, json));
    }

    /** Escreve eventos SSE; se a pessoa fechar a tela, para de escrever mas deixa o serviço terminar e gravar. */
    static final class SaidaSse implements LugiaSaida {
        private final HttpServletResponse resposta;
        private final ObjectMapper json;
        private PrintWriter escritor;
        private boolean desconectado;

        SaidaSse(HttpServletResponse resposta, ObjectMapper json) {
            this.resposta = resposta;
            this.json = json;
        }

        @Override
        public void abrir() {
            resposta.setStatus(HttpServletResponse.SC_OK);
            resposta.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resposta.setHeader("Cache-Control", "no-cache");
            resposta.setHeader("X-Accel-Buffering", "no");
            try {
                escritor = resposta.getWriter();
                resposta.flushBuffer();
            } catch (IOException e) {
                desconectado = true;
            }
        }

        @Override public void conversa(Long idConversa) { enviar("conversa", Collections.singletonMap("id", idConversa)); }
        @Override public void texto(String delta) { enviar("texto", Collections.singletonMap("delta", delta)); }
        @Override public void ferramenta(String descricao) { enviar("ferramenta", Collections.singletonMap("descricao", descricao)); }
        @Override public void fim(String motivo) { enviar("fim", Collections.singletonMap("motivo", motivo)); }
        @Override public void erro(String mensagem) { enviar("erro", Collections.singletonMap("mensagem", mensagem)); }

        private void enviar(String evento, Map<String, ?> dados) {
            if (desconectado || escritor == null) return;
            try {
                escritor.write("event: " + evento + "\ndata: " + json.writeValueAsString(dados) + "\n\n");
                escritor.flush();
                if (escritor.checkError()) desconectado = true;
            } catch (IOException e) {
                desconectado = true;
            }
        }
    }
}
