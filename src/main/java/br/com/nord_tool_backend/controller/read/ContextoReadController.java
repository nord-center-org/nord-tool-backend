package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.service.ContextoFaseService;
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
@RequestMapping("/api/v1/nord-tool/contexto")
@Tag(name = "Contexto", description = "Obras e fases que o usuário pode escolher")
public class ContextoReadController implements BaseResponse {

    private final ContextoFaseService service;

    @Operation(summary = "Obras e fases do usuário logado (o admin vê todas)")
    @GetMapping("/obras")
    public ResponseEntity<ApiResponseBody<List<ObraDto>>> minhasObras() {
        return ok(service.minhasObras());
    }
}
