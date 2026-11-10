package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.service.CubicagemService;
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
@RequestMapping("/api/v1/nord-tool/cubicagens")
@Tag(name = "Cubicagem", description = "Volume de concreto das peças de um projeto")
public class CubicagemReadController implements BaseResponse {

    private final CubicagemService service;

    @Operation(summary = "Minhas cubicagens (sem as peças)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<CubicagemDto>>> listar() {
        return ok(service.listar());
    }

    @Operation(summary = "Cubicagem com as peças")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<CubicagemDto>> abrir(@PathVariable Long id) {
        return ok(service.abrir(id));
    }
}
