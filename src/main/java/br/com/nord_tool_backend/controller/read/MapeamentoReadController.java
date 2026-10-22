package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.service.MapeamentoService;
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
@RequestMapping("/api/v1/nord-tool/mapeamentos")
@Tag(name = "Mapeamentos", description = "Contagens de campo e quantitativos")
public class MapeamentoReadController implements BaseResponse {

    private final MapeamentoService service;

    @Operation(summary = "Lista os mapeamentos das fases que o usuário vê (sem linhas)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<MapeamentoDto>>> listar() {
        return ok(service.listar());
    }

    @Operation(summary = "Mapeamento com as linhas")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<MapeamentoDto>> detalhar(@PathVariable Long id) {
        return ok(service.detalhar(id));
    }
}
