package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaModelosDto;
import br.com.nord_tool_backend.service.LugiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/lugia")
@Tag(name = "Lugia", description = "Chat com Claude, ChatGPT e Gemini e agente executor")
public class LugiaReadController implements BaseResponse {

    private final LugiaService service;

    @Operation(summary = "Modelos disponíveis, nível do usuário e se o modo executar está liberado")
    @GetMapping("/modelos")
    public ResponseEntity<ApiResponseBody<LugiaModelosDto>> modelos() {
        return ok(service.modelos());
    }

    @Operation(summary = "Conversas do usuário")
    @GetMapping("/conversas")
    public ResponseEntity<ApiResponseBody<List<LugiaConversaDto>>> conversas() {
        return ok(service.conversas());
    }

    @Operation(summary = "Mensagens da conversa (anexos sem o conteúdo)")
    @GetMapping("/conversas/{id}")
    public ResponseEntity<ApiResponseBody<List<LugiaMensagemDto>>> mensagens(@PathVariable Long id) {
        return ok(service.mensagens(id));
    }
}
