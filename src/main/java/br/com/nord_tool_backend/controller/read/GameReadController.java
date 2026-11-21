package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.GameConquistaDto;
import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GameResumoDto;
import br.com.nord_tool_backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/games")
@Tag(name = "Games", description = "Recordes, ranking, conquistas, favoritos e Copas")
public class GameReadController implements BaseResponse {

    private final GameService service;

    @Operation(summary = "Meus números, o ranking, as conquistas e o histórico de um jogo")
    @GetMapping("/resumo")
    public ResponseEntity<ApiResponseBody<GameResumoDto>> resumo(@RequestParam String jogo) {
        return ok(service.resumo(jogo));
    }

    @Operation(summary = "Todas as conquistas (as ganhas têm data)")
    @GetMapping("/conquistas")
    public ResponseEntity<ApiResponseBody<List<GameConquistaDto>>> conquistas() {
        return ok(service.conquistas());
    }

    @Operation(summary = "Meus jogos favoritos da Central")
    @GetMapping("/favoritos")
    public ResponseEntity<ApiResponseBody<List<String>>> favoritos() {
        return ok(service.favoritos());
    }

    @Operation(summary = "Minhas últimas Copas")
    @GetMapping("/copas")
    public ResponseEntity<ApiResponseBody<List<GameCopaDto>>> copas() {
        return ok(service.copas());
    }
}
