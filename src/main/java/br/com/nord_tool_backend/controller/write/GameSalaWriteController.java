package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.GameSalaDto;
import br.com.nord_tool_backend.form.GameSalaForm;
import br.com.nord_tool_backend.service.GameSalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/games/salas")
@Tag(name = "Games", description = "Salas do multiplayer (celular como controle)")
public class GameSalaWriteController implements BaseResponse {

    private final GameSalaService service;

    @Operation(summary = "Abre uma sala; quem abre hospeda na tela grande")
    @PostMapping
    public ResponseEntity<ApiResponseBody<GameSalaDto>> criar(@Valid @RequestBody GameSalaForm form) {
        return ok(service.criar(form.getJogo()));
    }

    @Operation(summary = "Entra (ou volta) numa sala pelo código e recebe o bilhete do WebSocket")
    @PostMapping("/{codigo}/entrar")
    public ResponseEntity<ApiResponseBody<GameSalaDto>> entrar(@PathVariable String codigo) {
        return ok(service.entrar(codigo));
    }
}
