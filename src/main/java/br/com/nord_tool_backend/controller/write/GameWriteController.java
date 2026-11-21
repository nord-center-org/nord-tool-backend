package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GameRegistroDto;
import br.com.nord_tool_backend.form.GameCopaForm;
import br.com.nord_tool_backend.form.GameFavoritosForm;
import br.com.nord_tool_backend.form.GamePartidaForm;
import br.com.nord_tool_backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/games")
@Tag(name = "Games", description = "Recordes, ranking, conquistas, favoritos e Copas")
public class GameWriteController implements BaseResponse {

    private final GameService service;

    @Operation(summary = "Registra o resultado de uma partida e devolve recorde e conquistas novas")
    @PostMapping("/partidas")
    public ResponseEntity<ApiResponseBody<GameRegistroDto>> registrarPartida(@Valid @RequestBody GamePartidaForm form) {
        return ok(service.registrarPartida(form));
    }

    @Operation(summary = "Troca a lista de jogos favoritos (a tela manda a lista inteira)")
    @PutMapping("/favoritos")
    public ResponseEntity<ApiResponseBody<List<String>>> salvarFavoritos(@Valid @RequestBody GameFavoritosForm form) {
        return ok(service.salvarFavoritos(form.getJogos()));
    }

    @Operation(summary = "Registra o fim de uma Copa (3, 5 ou 7 rodadas)")
    @PostMapping("/copas")
    public ResponseEntity<ApiResponseBody<GameCopaDto>> registrarCopa(@Valid @RequestBody GameCopaForm form) {
        return ok(service.registrarCopa(form));
    }
}
