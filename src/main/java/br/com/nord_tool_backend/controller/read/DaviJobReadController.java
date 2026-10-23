package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.DaviJobPlacarDto;
import br.com.nord_tool_backend.service.DaviJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/davi-job")
@Tag(name = "Davi Job", description = "Placar do minigame Caça ao rato")
public class DaviJobReadController implements BaseResponse {

    private final DaviJobService service;

    @Operation(summary = "Top 10 (melhor pontuação de cada pessoa) e o recorde do usuário logado")
    @GetMapping("/placar")
    public ResponseEntity<ApiResponseBody<DaviJobPlacarDto>> placar() {
        return ok(service.placar());
    }
}
