package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.IntegracaoTokenDto;
import br.com.nord_tool_backend.form.IntegracaoTokenForm;
import br.com.nord_tool_backend.service.IntegracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/integracoes")
@Tag(name = "Integrações", description = "Links de leitura para assistentes de IA (Gemini, ChatGPT…)")
public class IntegracaoWriteController implements BaseResponse {

    private final IntegracaoService service;

    @Operation(summary = "Cria um link de leitura do cronograma; a chave só aparece nesta resposta")
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponseBody<IntegracaoTokenDto>> criar(@Valid @RequestBody IntegracaoTokenForm form) {
        return created(service.criarToken(form));
    }

    @Operation(summary = "Revoga um link (para de funcionar na hora)")
    @DeleteMapping("/tokens/{id}")
    public ResponseEntity<ApiResponseBody<Void>> revogar(@PathVariable Long id) {
        service.revogarToken(id);
        return noContent();
    }
}
