package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.NotificacaoCaixaDto;
import br.com.nord_tool_backend.dto.NotificacaoConfigDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/notificacoes")
@Tag(name = "Notificações", description = "Notificações no celular (Web Push)")
public class NotificacaoReadController implements BaseResponse {

    private final NotificacaoService service;

    @Operation(summary = "Chave pública VAPID para o navegador se inscrever")
    @GetMapping("/config")
    public ResponseEntity<ApiResponseBody<NotificacaoConfigDto>> config() {
        return ok(service.config());
    }

    @Operation(summary = "Aparelhos da pessoa com as notificações ativadas")
    @GetMapping("/aparelhos")
    public ResponseEntity<ApiResponseBody<List<PushInscricaoDto>>> aparelhos() {
        return ok(service.meusAparelhos());
    }

    @Operation(summary = "O que a pessoa quer receber")
    @GetMapping("/preferencias")
    public ResponseEntity<ApiResponseBody<NotificacaoPreferenciaDto>> preferencias() {
        return ok(service.preferencias());
    }

    @Operation(summary = "Caixa de notificações: avisos depois de um número de sequência (o app Android lê em segundo plano)")
    @GetMapping("/caixa")
    public ResponseEntity<ApiResponseBody<NotificacaoCaixaDto>> caixa(@RequestParam(required = false) Long depois) {
        return ok(service.caixa(depois));
    }
}
