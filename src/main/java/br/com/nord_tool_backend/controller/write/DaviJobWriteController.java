package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.DaviJobPlacarDto;
import br.com.nord_tool_backend.form.DaviJobPontuacaoForm;
import br.com.nord_tool_backend.service.DaviJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/davi-job")
@Tag(name = "Davi Job", description = "Placar do minigame Caça ao rato")
public class DaviJobWriteController implements BaseResponse {

    private final DaviJobService service;

    @Operation(summary = "Registra a pontuação de uma partida e devolve o placar")
    @PostMapping("/pontuacoes")
    public ResponseEntity<ApiResponseBody<DaviJobPlacarDto>> registrar(@Valid @RequestBody DaviJobPontuacaoForm form) {
        return created(service.registrar(form));
    }
}
