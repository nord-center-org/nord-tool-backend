package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MeusDadosDto;
import br.com.nord_tool_backend.service.MeusDadosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/conta/dados")
@Tag(name = "Conta", description = "Os dados da própria pessoa")
public class MeusDadosReadController implements BaseResponse {

    private final MeusDadosService service;

    @Operation(summary = "Meus dados (nome, e-mail, celular, empresa e cargo)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<MeusDadosDto>> ler() {
        return ok(service.ler());
    }
}
