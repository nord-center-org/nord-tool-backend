package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.form.NotificacaoPreferenciaForm;
import br.com.nord_tool_backend.form.PushCancelarForm;
import br.com.nord_tool_backend.form.PushInscricaoForm;
import br.com.nord_tool_backend.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/notificacoes")
@Tag(name = "Notificações", description = "Notificações no celular (Web Push)")
public class NotificacaoWriteController implements BaseResponse {

    private final NotificacaoService service;

    @Operation(summary = "Ativa as notificações neste aparelho (inscrição do navegador)")
    @PostMapping("/aparelhos")
    public ResponseEntity<ApiResponseBody<Void>> inscrever(@Valid @RequestBody PushInscricaoForm form) {
        service.inscrever(form);
        return noContent();
    }

    @Operation(summary = "Desativa as notificações neste aparelho")
    @PostMapping("/aparelhos/cancelar")
    public ResponseEntity<ApiResponseBody<Void>> cancelar(@Valid @RequestBody PushCancelarForm form) {
        service.cancelar(form.getEndpoint());
        return noContent();
    }

    @Operation(summary = "Salva o que a pessoa quer receber")
    @PutMapping("/preferencias")
    public ResponseEntity<ApiResponseBody<NotificacaoPreferenciaDto>> preferencias(@Valid @RequestBody NotificacaoPreferenciaForm form) {
        return ok(service.salvarPreferencias(form));
    }

    @Operation(summary = "Manda um aviso de teste para os aparelhos da pessoa")
    @PostMapping("/teste")
    public ResponseEntity<ApiResponseBody<Integer>> testar() {
        return ok(service.testar());
    }
}
