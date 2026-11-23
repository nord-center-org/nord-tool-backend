package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.DrumcamBilheteDto;
import br.com.nord_tool_backend.form.DrumcamBilheteForm;
import br.com.nord_tool_backend.service.DrumcamService;
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
@RequestMapping("/api/v1/nord-tool/drumcam")
@Tag(name = "Drumcam", description = "Controle do celular-câmera pela internet")
public class DrumcamWriteController implements BaseResponse {

    private final DrumcamService service;

    @Operation(summary = "Bilhete de uso único para abrir o WebSocket como câmera ou controle")
    @PostMapping("/bilhete")
    public ResponseEntity<ApiResponseBody<DrumcamBilheteDto>> bilhete(@Valid @RequestBody DrumcamBilheteForm form) {
        return ok(service.bilhete(form.getPapel(), form.getNome()));
    }
}
