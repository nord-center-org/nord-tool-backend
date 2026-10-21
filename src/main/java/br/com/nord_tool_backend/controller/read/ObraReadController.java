package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.service.ObraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/administracao")
@Tag(name = "Obras e fases", description = "Cadastro de obras, fases e participantes (só administradores)")
public class ObraReadController implements BaseResponse {

    private final ObraService service;

    @Operation(summary = "Obras com as fases e os participantes de cada fase")
    @GetMapping("/obras")
    public ResponseEntity<ApiResponseBody<List<ObraDto>>> listar() {
        return ok(service.listar());
    }
}
