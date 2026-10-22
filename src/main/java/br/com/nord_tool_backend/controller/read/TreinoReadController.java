package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.service.TreinoService;
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
@RequestMapping("/api/v1/nord-tool/treinos")
@Tag(name = "Treinos", description = "Fichas de treino e sessões (dados do usuário logado)")
public class TreinoReadController implements BaseResponse {

    private final TreinoService service;

    @Operation(summary = "Fichas de treino do usuário logado, com exercícios e última sessão")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<TreinoDto>>> listar() {
        return ok(service.listar());
    }

    @Operation(summary = "Últimas sessões de treino (padrão 30, máximo 200)")
    @GetMapping("/sessoes")
    public ResponseEntity<ApiResponseBody<List<TreinoSessaoDto>>> sessoes(@RequestParam(value = "limite", required = false) Integer limite) {
        return ok(service.listarSessoes(limite));
    }
}
