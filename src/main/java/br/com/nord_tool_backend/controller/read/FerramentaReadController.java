package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.service.FerramentaService;
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
@RequestMapping("/api/v1/nord-tool/ferramentas")
@Tag(name = "Ferramentas", description = "Endpoints para buscar e listar ferramentas")
public class FerramentaReadController implements BaseResponse {
    private final FerramentaService ferramentaService;

    @Operation(summary = "Listar ferramentas")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<FerramentaDto>>> listarFerramentas() {
        return ok(ferramentaService.listarFerramentas());
    }

    @Operation(summary = "Buscar ferramenta por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<FerramentaDto>> buscarFerramenta(@PathVariable Long id) {
        return ok(ferramentaService.buscarPorIdFerramenta(id));
    }
}
