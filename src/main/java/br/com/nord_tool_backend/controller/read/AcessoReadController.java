package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.service.AcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/administracao/acessos")
@Tag(name = "Acessos", description = "Perfis e acessos por usuário (só administradores)")
public class AcessoReadController implements BaseResponse {

    private final AcessoService service;

    @Operation(summary = "Colaboradores da base antiga e o usuário vinculado a cada um")
    @GetMapping("/colaboradores-legados")
    public ResponseEntity<ApiResponseBody<java.util.List<br.com.nord_tool_backend.dto.ColaboradorLegadoDto>>> legados() {
        return ok(service.listarColaboradoresLegados());
    }

    @Operation(summary = "Usuários com acessos efetivos, perfis e módulos")
    @GetMapping
    public ResponseEntity<ApiResponseBody<AcessosDto>> listar() {
        return ok(service.listar());
    }
}
